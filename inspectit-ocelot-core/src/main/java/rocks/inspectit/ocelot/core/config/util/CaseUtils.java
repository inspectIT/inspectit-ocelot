package rocks.inspectit.ocelot.core.config.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CaseUtils {

    /**
     * Converts the given camelCase String to kebab-case.
     * Any other separator characters are note affected.
     *
     * @param str the string in camelCase
     * @return the string in kebab-case
     */
    public String camelCaseToKebabCase(String str) {
        int position = 0;
        for (int i = 0; i < str.length() - 1; i++) {
            char first = str.charAt(i);
            char second = str.charAt(i + 1);
            if (Character.isLowerCase(first) && Character.isUpperCase(second)) {
                str = str.substring(0, i + 1) + "-" + Character.toLowerCase(second) + str.substring(i + 2);
            }
        }
        return str;
    }
}
