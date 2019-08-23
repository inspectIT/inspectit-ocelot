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
import java.util.stream.Collectors;

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
            List<String> ls = propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> findInvalidPropertyNames(ps.getPropertyNames()).stream())
                    .collect(Collectors.toList());
            for (String s : ls) {
                log.warn("Expression could not be resolved to a property: " + s);
            }

        });
    }

    /**
     * Method to determine which given properties do not exists in inspectIt
     * Used to track down spelling errors
     *
     * @param propertyNames A String Array containing the parameters one wants to validate, e.g. 'inspectit.service-name'
     * @return A List of Strings contained in the given Array which could not be resolved to a property.
     */
    public List<String> findInvalidPropertyNames(String[] propertyNames) {
        ArrayList<String> unmappedStrings = new ArrayList<>();
        for (String propertyName : propertyNames) {
            if (checkPropertyName(propertyName)) {
                unmappedStrings.add(propertyName);
            }
        }
        return unmappedStrings;
    }

    /**
     * Checks if a propertyName should be added to the List of unmappedStrings or not
     *
     * @param propertyName
     * @return True: the propertyName does not exists as path <br> False: the propertyName exists as path
     */
    boolean checkPropertyName(String propertyName) {
        return propertyName != null
                && propertyName.length() > 8
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
        ArrayList<String> ls = new ArrayList<>();
        if (propertyName == null || propertyName.isEmpty()) {
            return ls;
        }
        char expressionEnd = '.';
        if (propertyName.charAt(0) == '[') {
            expressionEnd = ']';
        }
        ls.add(cleanString(propertyName.substring(0, grabExpression(propertyName, expressionEnd))));
        ls.addAll(parse(propertyName.substring(grabExpression(propertyName, expressionEnd))));
        ls.remove("inspectit");
        return ls;
    }

    /**
     * Takes a String and a char which resembles the end of an expression, returns the index on which the found expression
     * ends in the string
     *
     * @param propertyName  String in which an expression should be search
     * @param expressionEnd Marker for the end of the expression
     * @return the index at which the expression ends
     */
    private int grabExpression(String propertyName, char expressionEnd) {
        int i = 0;
        boolean isFirst = true;
        for (char charInProperty : propertyName.toCharArray()) {
            if (!isFirst) {
                if (expressionEnd == '.' && charInProperty == expressionEnd || charInProperty == '[') {
                    return i;
                } else if (charInProperty == expressionEnd) {
                    return i + 1;
                }
            }
            isFirst = false;
            i++;
        }
        return propertyName.length();
    }

    /**
     * Removes literals such as '.', '[' or ']' from Strings
     *
     * @param unclean String which should be cleaned
     * @return cleaned String
     */
    private String cleanString(String unclean) {
        String cleanString;
        if (unclean.contains("[")) {
            cleanString = unclean.replace("[", "");
            cleanString = cleanString.replace("]", "");
        } else {
            cleanString = unclean.replace(".", "");
        }
        return cleanString;
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
        String nameToReturn = name;
        String[] nameParts = name.split("-");
        boolean isFirst = true;
        for (String part : nameParts) {
            if (isFirst) {
                nameToReturn = part.toLowerCase();
                isFirst = false;
            } else if (!part.isEmpty()) {
                part = part.toLowerCase();
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
                nameToReturn += part;
            }
        }
        return nameToReturn;
    }


    /**
     * Checks if a given List of properties exists as path
     *
     * @param propertyNames The list of properties one wants to check
     * @param type          The type in which the current top-level properties should be found
     * @return True: when the property exsits <br> False: when it doesn't
     */
    private boolean checkPropertyExists(List<String> propertyNames, Type type) {
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
    private boolean checkPropertyExistsInMap(List<String> propertyNames, Type mapValueType) {
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
    private boolean checkPropertyExistsInList(List<String> propertyNames, Type listValueType) {
        return checkPropertyExists(propertyNames.subList(1, propertyNames.size()), listValueType);
    }

    /**
     * Checks if the first entry of the propertyNames list exists as property in a given bean
     *
     * @param propertyNames List of property names
     * @param beanType      The bean through which should be searched
     * @return True: the property and all other properties exists <br> False: At least one of the properties does not exist
     */
    private boolean checkPropertyExistsInBean(List<String> propertyNames, Class<?> beanType) {
        String propertyName = toCamelCase(propertyNames.get(0));
        Optional<PropertyDescriptor> foundProperty =
                Arrays.stream(BeanUtils.getPropertyDescriptors(beanType))
                        .filter(descriptor -> descriptor.getName().equals(propertyName))
                        .findFirst();
        if (foundProperty.isPresent()) {
            //Am Ende des Pfades angelangt?
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
