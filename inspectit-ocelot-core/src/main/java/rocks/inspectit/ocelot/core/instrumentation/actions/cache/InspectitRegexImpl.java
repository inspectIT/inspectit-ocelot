package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitRegex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Caches regex patterns for further use.
 */
public class InspectitRegexImpl implements InspectitRegex {

    /**
     * The name of this bean, initialized via the {@link rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration}
     */
    public static final String BEAN_NAME = "inspectitRegex";

    /** Cached patterns */
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    @Override
    public boolean matches(String regex, String string) {
        return matcher(regex, string).matches();
    }

    @Override
    public Matcher matcher(String regex, String string) {
        Pattern pattern = pattern(regex);
        return pattern.matcher(string);
    }

    @Override
    public Pattern pattern(String regex) {
        return patterns.computeIfAbsent(regex, Pattern::compile);
    }
}
