package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import rocks.inspectit.ocelot.bootstrap.exposed.RegexCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RegexCacheImpl implements RegexCache {

    /**
     * The name of this bean, initialized via the {@link rocks.inspectit.ocelot.core.config.spring.BootstrapInitializerConfiguration}
     */
    public static final String BEAN_NAME = "regexCache";

    /** Cached patterns */
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    @Override
    public Boolean matches(String regex, String toTest) {
        Pattern pattern = pattern(regex);
        return pattern.matcher(toTest).matches();
    }

    @Override
    public Pattern pattern(String regex) {
        return patterns.computeIfAbsent(regex, Pattern::compile);
    }
}
