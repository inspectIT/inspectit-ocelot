package rocks.inspectit.ocelot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Component
public class YamlValidator {

    @Autowired
    private InspectitEnvironment env;

    /**
     * A HashSet of classes which are used as wildcards in the search for properties. If a found class matches one of these
     * classes, the end of the property path is reached. Mainly used in the search of maps
     */
    private static final HashSet<Class<?>> WILDCARD_TYPES = new HashSet(Arrays.asList(Object.class, String.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, Void.class,
            Boolean.class, Byte.class, Short.class));

    @PostConstruct
    public void startStringFinder() {
        env.readPropertySources(propertySources -> {
            propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .filter(ps -> checkPropertyName(ps))
                    .forEach(ps -> log.warn("Expression could not be resolved to a property: " + ps));
        });
    }

    /**
     * Checks if a propertyName should be added to the List of unmappedStrings or not
     *
     * @param propertyName
     * @return True: the propertyName does not exists as path <br> False: the propertyName exists as path
     */
    boolean checkPropertyName(String propertyName) {
        return propertyName != null
                && propertyName.startsWith("inspectit.")
                && !checkPropertyExists(parse(propertyName), InspectitConfig.class);
    }

    /**
     * Helper method for findUnmappedStrings
     * This method takes an array of strings and returns each entry as ArrayList containing the parts of each element.
     * <p>
     * 'inspectit.hello-i-am-testing' would be returned as {'inspectit', 'helloIAmTesting'}
     *
     * @param propertyName A String Array containing property Strings
     * @return a ArrayList containing containing the parts of the property as String
     */
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
     * The remaidner of the proeprty name is returned
     * <p>
     * E.g. inspectit.test.rest -> "inspectit" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal].test.rest -> "inspectit.literal" is added to the list, "test.rest" is returned.
     * E.g. [inspectit.literal][test].rest -> "inspectit.literal" is added to the list, "[test].rest" is returned.
     *
     * @param propertyName
     * @param result
     * @return
     */
    String extractExpression(String propertyName, List<String> result) {
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
     * Helper-Method for parse
     * Takes any given String and converts it from kebab-case into camelCase
     * Strings without any dashes are returned unaltered
     *
     * @param name The String which should be changed into camelCase
     * @return the given String in camelCase
     */
    String toCamelCase(String name) {
        StringBuilder builder = new StringBuilder();
        String[] nameParts = name.split("-");
        boolean isFirst = true;
        for (String part : nameParts) {
            if (isFirst) {
                builder.append(part.toLowerCase());
                isFirst = false;
            } else if (!part.isEmpty()) {
                part = part.toLowerCase();
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
                builder.append(part);
            }
        }
        return builder.toString();
    }


    /**
     * Checks if a given List of properties exists as path
     *
     * @param propertyNames The list of properties one wants to check
     * @param type          The type in which the current top-level properties should be found
     * @return True: when the property exsits <br> False: when it doesn't
     */
    boolean checkPropertyExists(List<String> propertyNames, Type type) {
        propertyNames.remove("inspectit");
        if (propertyNames.isEmpty()) {
            return true; //base case
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
    boolean checkPropertyExistsInMap(List<String> propertyNames, Type mapValueType) {
        if (WILDCARD_TYPES.contains(mapValueType)) {
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
    boolean checkPropertyExistsInList(List<String> propertyNames, Type listValueType) {
        return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), listValueType);
    }

    /**
     * Checks if the first entry of the propertyNames list exists as property in a given bean
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return True: the property and all other properties exists <br> False: At least one of the properties does not exist
     */
    boolean checkPropertyExistsInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = toCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> descriptor.getName().equals(propertyName))
                        .findFirst();
        if (foundProperty.isPresent()) {
            if (foundProperty.get().getReadMethod() == null) {
                return true;
            }
            Type propertyType = foundProperty.get().getReadMethod().getGenericReturnType();
            return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), propertyType);
        } else {
            return false;
        }
    }
}
