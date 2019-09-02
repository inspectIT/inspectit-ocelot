package rocks.inspectit.ocelot.core.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.utils.CaseUtils;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class PropertyNamesValidator {

    @Autowired
    private InspectitEnvironment env;


    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> TERMINAL_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class, Duration.class, Path.class, URL.class, FileSystemResource.class));

    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    public void logInvalidPropertyNames() {
        env.readPropertySources(propertySources -> {
            propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .filter(ps -> checkPropertyName(ps))
                    .forEach(ps -> log.warn("The specified property '{}' does not exist! ", ps));
        });
    }

    /**
     * Checks if a given property is an inspectit-property and is a valid path in the config model
     * This method firstly checks if a given String fulfills the basic requirements: being not null and starting with "inspectit."
     * If a String does not fulfill these basic requirements the method returns false
     * If these checks are successful, a process for recursive path-checking is triggered with checkPropertyExists.
     * This process checks each element of the given path for existence. Upon on the first occurrence of a non-existing path,
     * false is returned. If the path exists, true is returned
     *
     * @param propertyName
     * @return True: the propertyName exists as path <br> False: the propertyName does not exist as path
     */
    @VisibleForTesting
    boolean checkPropertyName(String propertyName) {
        ArrayList<String> parsedName = (ArrayList<String>) parse(propertyName);
        try {
            return propertyName != null
                    && !propertyName.startsWith("inspectit.publishOpenCensusToBootstrap")
                    && propertyName.startsWith("inspectit.")
                    && !checkPropertyExists(parsedName.subList(1, parsedName.size()), InspectitConfig.class);

        } catch (Exception e) {
            log.error("Error while checking property existence", e);
        }
        return false;
    }

    /**
     * This method takes an array of strings and returns each entry as ArrayList containing the parts of each element.
     * <p>
     * 'inspectit.hello-i-am-testing' would be returned as {'inspectit', 'helloIAmTesting'}
     *
     * @param propertyName A String containing the property path
     * @return a List containing containing the parts of the property path as String
     */
    @VisibleForTesting
    List<String> parse(String propertyName) {
        ArrayList<String> result = new ArrayList<>();
        String remainder = propertyName;
        while (remainder != null && !remainder.isEmpty()) {
            remainder = extractExpression(remainder, result);
        }
        return result;
    }

    /**
     * Extracts the first path expression from the given propertyName and appends it to the given result list.
     * The remainder of the property name is returned
     * <p>
     * E.g. inspectit.test.rest -> "inspectit" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal].test.rest -> "inspectit.literal" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal][test].rest -> "inspectit.literal" is added to the list, "[test].rest" is returned.
     *
     * @param propertyName A String with the path of a property
     * @param result       Reference to the list in which the extracted expressions should be saved in
     * @return the remaining expression
     */
    private String extractExpression(String propertyName, List<String> result) {
        if (propertyName.startsWith("[")) {
            int end = propertyName.indexOf(']');
            if (end == -1) {
                throw new IllegalArgumentException("invalid property path");
            }
            result.add(propertyName.substring(1, end));
            return removeLeadingDot(propertyName.substring(end + 1));
        } else {
            int end = findFirstIndexOf(propertyName, '.', '[');
            if (end == -1) {
                result.add(propertyName);
                return "";
            } else {
                result.add(propertyName.substring(0, end));
                return removeLeadingDot(propertyName.substring(end));
            }
        }
    }

    private int findFirstIndexOf(String propertyName, char first, char second) {
        int firstIndex = propertyName.indexOf(first);
        int secondIndex = propertyName.indexOf(second);
        if (firstIndex == -1) {
            return secondIndex;
        } else if (secondIndex == -1) {
            return firstIndex;
        } else {
            return Math.min(firstIndex, secondIndex);
        }
    }

    private String removeLeadingDot(String string) {
        if (string.startsWith(".")) {
            return string.substring(1);
        } else {
            return string;
        }
    }

    /**
     * Checks if a given List of properties exists as path
     *
     * @param propertyNames The list of properties one wants to check
     * @param type          The type in which the current top-level properties should be found
     * @return True: when the property exsits <br> False: when it doesn't
     */
    @VisibleForTesting
    boolean checkPropertyExists(List<String> propertyNames, Type type) {
        if (propertyNames.isEmpty()) {
            return isTerminal(type) || isListOfTerminalTypes(type);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == Map.class) {
                return checkPropertyExistsInMap(propertyNames, genericType.getActualTypeArguments()[1]);
            } else if (genericType.getRawType() == List.class) {
                return checkPropertyExistsInList(propertyNames, genericType.getActualTypeArguments()[0]);
            }
        }
        if (type instanceof Class) {
            return checkPropertyExistsInBean(propertyNames, (Class<?>) type);
        } else {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
    }

    /**
     * Checks if a given type exists as value type in a map, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param mapValueType  The type which is given as value type of a map
     * @return True: The type exists <br> False: the type does not exists
     */
    @VisibleForTesting
    boolean checkPropertyExistsInMap(List<String> propertyNames, Type mapValueType) {
        if (isTerminal(mapValueType)) {
            return true;
        } else {
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), mapValueType);
        }
    }

    /**
     * Checks if a given type exists as value type in a list, keeps crawling through the given propertyName list
     *
     * @param propertyNames List of property names
     * @param listValueType The type which is given as value type of a list
     * @return True: The type exists <br> False: the type does not exists
     */
    @VisibleForTesting
    boolean checkPropertyExistsInList(List<String> propertyNames, Type listValueType) {
        if (isTerminal(listValueType)) {
            return true;
        } else {
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), listValueType);
        }
    }

    /**
     * Checks if the first entry of the propertyNames list exists as property in a given bean
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return True: the property and all other properties exists <br> False: At least one of the properties does not exist
     */
    private boolean checkPropertyExistsInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = CaseUtils.kebabCaseToCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> descriptor.getName().equalsIgnoreCase(propertyName))
                        .findFirst();
        if (foundProperty.isPresent()) {
            Type propertyType;
            if (foundProperty.get().getReadMethod() != null) {
                propertyType = foundProperty.get().getReadMethod().getGenericReturnType();
            } else {
                propertyType = foundProperty.get().getPropertyType();
            }
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), propertyType);
        } else {
            return false;
        }
    }

    /**
     * Checks if a given type is a terminal type or an enum
     *
     * @param type
     * @return True: the given type is a terminal or an enum False: the given type is neither a terminal type nor an enum
     */
    private boolean isTerminal(Type type) {
        if (TERMINAL_TYPES.contains(type)) {
            return true;
        } else if (type instanceof Class) {
            return ((Class<?>) type).isEnum() || ((Class<?>) type).isPrimitive();
        }
        return false;
    }


    /**
     * Checks if a given type is a list of terminal types
     *
     * @param type
     * @return True: the given type is a list of a terminal type. False: either the given type is not a list or not a list of terminal types
     */
    private boolean isListOfTerminalTypes(Type type) {
        if (type instanceof ParameterizedType) {
            int typeIndex = 0;
            ParameterizedType genericType = (ParameterizedType) type;
            if (genericType.getRawType() == Map.class) {
                typeIndex = 1;
            }
            return isTerminal(genericType.getActualTypeArguments()[typeIndex]);
        }
        return false;
    }
}