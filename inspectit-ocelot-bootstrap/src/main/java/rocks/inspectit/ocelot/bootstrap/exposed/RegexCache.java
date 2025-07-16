package rocks.inspectit.ocelot.bootstrap.exposed;

import java.util.regex.Pattern;

/**
 * The regex API, which is accessible from actions.
 * A specialized cache specifically for regex patterns.
 */
public interface RegexCache {

    /**
     * @param regex the regex expression
     * @param toTest the string to test for the expression
     *
     * @return true if the provided string matches the expression
     */
    Boolean matches(String regex, String toTest);

    /**
     * Compiles and caches the pattern.
     *
     * @param regex the regex expression
     *
     * @return the compiled regex pattern for the provided expression
     */
    Pattern pattern(String regex);
}
