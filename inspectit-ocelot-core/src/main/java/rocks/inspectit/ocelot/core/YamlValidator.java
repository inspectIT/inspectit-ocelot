package rocks.inspectit.ocelot.core;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;

import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import lombok.extern.slf4j.Slf4j;


import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YamlValidator {

     @Autowired
     private InspectitEnvironment env;

    private static final Class<?>[] WILDCARD_TYPES = {Object.class, String.class,Integer.class, Long.class,
                                                       Float.class, Double.class, Character.class, Void.class,
                                                       Boolean.class, Byte.class, Short.class};

    @PostConstruct
    public void startStringFinder(){
        env.readPropertySources(propertySources -> {
            List<String> ls = propertySources.stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource)
                    .map(ps -> (EnumerablePropertySource) ps)
                    .flatMap(ps -> this.findUnmappedStrings(ps.getPropertyNames()).stream())
                     .collect(Collectors.toList());
            for (String s: ls
                 ) {
                log.warn("Expression could not be resolved to a property: " + s);
        }

        });
    }

    /**
     * Method to determine which given properties do not exists in inspectIt
     * Used to track down spelling errors
     *
     * @param toMap A String Array containing the parameters one wants to validate, e.g. 'inspectit.service-name'
     * @return A List of Strings contained in the given Array which could not be resolved to a property.
     */
    //TODO Lesbarkeit verbessern -> Parse nur für einzelne Strings!
    public LinkedList<String> findUnmappedStrings(String[] toMap){
        LinkedList<String> unmappedStrings = new LinkedList<>();
        for (String propertyName: toMap
             ) {
            if(propertyName.length() > 8 && propertyName.substring(0, 9).equals("inspectit")) {
                if (!existsPropertyName(this.parse(propertyName), InspectitConfig.class))
                    unmappedStrings.add(propertyName);
            }
        }
        return unmappedStrings;
    }

    /**
     * Helper method for findUnmappedStrings
     * This method takes an array of strings, parses kebab-cases or snake-case into camelCase and returns each entry as LinkedList
     * containing the parts of each element.
     *
     * 'inspectit.hello-i-am-testing' would be returned as {'inspectit', 'helloIAmTesting'}
     *
     * @param input A String Array containing property Strings
     * @return a LinkedList containing LinkedLists containing the parts of the property as String
     */
    private List<String> parse(String input) {
        char currentChar;
        boolean isFirst = true;
        boolean inBrackets = false;
        int currentListIndex = 0;
        LinkedList<LinkedList<String>> list = new LinkedList<>();
        String foundExpression = "";
        LinkedList<String> toAdd = new LinkedList<>();
        for (int j = 0; j < input.length(); j++) {
            currentChar = input.charAt(j);
                switch (currentChar) {
                    case ('.'):
                        if (!inBrackets) {
                            if(!isFirst && !"".equals(foundExpression)) toAdd.add(foundExpression);
                            else isFirst = false;
                            foundExpression = "";
                        } else {
                            foundExpression += currentChar;
                        }
                        break;
                    case ('['):
                        inBrackets = true;
                        if(!isFirst) toAdd.add(foundExpression);
                        else isFirst = false;
                        foundExpression = "";
                        break;
                    case (']'):
                        inBrackets = false;
                        if(!isFirst) toAdd.add(foundExpression);
                        else isFirst = false;
                        foundExpression = "";
                        break;
                    default:
                        foundExpression += currentChar;


                }

        }
        if(!foundExpression.equals("") && !isFirst) toAdd.add(foundExpression);
        return toAdd;
    }

    /**
     * Helper-Method for parse
     * Takes any given String and converts it from kebab-case into camelCase
     * Strings without any dashes are returned unaltered
     *
     * @param name The String which should be changed into camelCase
     * @return the given String in camelCase
     */
    private String toCamelCase(String name){
        String nameToReturn = name;
        String splitRegex = null;
        String className = null;
        if(name.contains("-")) splitRegex = "-";
        if(name.contains("_")) splitRegex = "_";
        if(splitRegex != null) {
            String[] nameParts = name.split(splitRegex);
            boolean isFirst = true;
            for (String part : nameParts) {
                if (isFirst) {
                    nameToReturn = part.toLowerCase();
                    isFirst = false;
                } else {
                    part = part.toLowerCase();
                    part = part.substring(0, 1).toUpperCase() + part.substring(1);
                    nameToReturn += part;
                }
            }

        }
        return nameToReturn;
    }


    /**
     * Checks if a given List of property names exists as a directory of variables
     * @param propertyNames The list of properties one wants to check
     * @param currentBean The bean in which the top-level properties should be found
     * @return True: when the property exsits <br> False: when it doenst
     */
    private boolean existsPropertyName(List<String> propertyNames, Class<?> currentBean){
        if(propertyNames.size() == 0) return true;
        int nextStartIndex = 1;
        String currentString = propertyNames.get(0);
        PropertyDescriptor foundProperty = null;
        Class<?> nextClass = null;
        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(currentBean);
        for (PropertyDescriptor descriptor : descriptors
            ) {
                if (descriptor.getName().equals(this.toCamelCase(currentString))) foundProperty = descriptor;
        }
        if (foundProperty == null) return false;
        if(Map.class.equals(foundProperty.getPropertyType())){
            nextClass = (Class<?>) ((ParameterizedType) foundProperty.getReadMethod().getGenericReturnType()).getActualTypeArguments()[1];
            nextStartIndex = 2;

        }else if(List.class.equals(foundProperty.getPropertyType())) {
            nextClass = (Class<?>) ((ParameterizedType) foundProperty.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
            nextStartIndex = 2;
        }else{
            nextClass = foundProperty.getPropertyType();
        }
        if (propertyNames.size() <= 1 || isWildCardType(nextClass)) return true;
        return existsPropertyName(propertyNames.subList(nextStartIndex, propertyNames.size()), nextClass);
     }

     private boolean isWildCardType(Class<?> currentBean){
         for (Class <?> cl: WILDCARD_TYPES
              ) {
             if(cl.equals(currentBean)) return true;
         }
         return false;
     }

}


