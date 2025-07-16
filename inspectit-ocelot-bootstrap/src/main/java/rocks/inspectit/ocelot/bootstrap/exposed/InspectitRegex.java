package rocks.inspectit.ocelot.bootstrap.exposed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The regex API, which is accessible from actions.
 * A specialized cache specifically for regex patterns.
 */
public interface InspectitRegex {

    /**
     * @param regex the regex expression
     * @param string the string to test for the expression
     *
     * @return true if the provided string matches the expression
     */
    boolean matches(String regex, String string);

    /**
     *
     * @param regex the regex expression
     * @param string the string to test for the expression
     *
     * @return the matcher for the provided regex expression and string
     */
    Matcher matcher(String regex, String string);

    /**
     * Compiles and caches the pattern.
     *
     * @param regex the regex expression
     *
     * @return the compiled regex pattern for the provided expression
     */
    Pattern pattern(String regex);
}
