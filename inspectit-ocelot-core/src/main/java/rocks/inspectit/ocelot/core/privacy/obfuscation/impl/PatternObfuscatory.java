package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

@AllArgsConstructor
public class PatternObfuscatory implements IObfuscatory {

    private static final Function<String, Object> DEFAULT_OBFUSCATION_FUNCTION = (v) -> "***";

    /**
     * Patterns that need to be checked
     */
    private final Collection<PatternEntry> patternEntries;

    /**
     * Map holding already checked keys as key and info if the data should be obfuscated or not
     */
    private final Cache<String, CheckedKeyObfuscationValue> checkedKeysMap = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSpanAttribute(Span span, String key, Object value) {
        String strValue = value.toString();
        Object obfuscatedValue = shouldObfuscate(key, strValue)
                .map(function -> function.apply(strValue))
                .orElse(value);

        IObfuscatory.super.putSpanAttribute(span, key, obfuscatedValue);
    }

    private Optional<Function<String, Object>> shouldObfuscate(String key, String value) {
        // get info from the map
        CheckedKeyObfuscationValue keyObfuscationValue = checkedKeysMap.getIfPresent(key);

        // if we know this should be obfuscated return immediately with function that was saved
        if (null != keyObfuscationValue && keyObfuscationValue.shouldObfuscate) {
            return Optional.of(keyObfuscationValue.obfuscationFunction);
        }

        // if we have no information about this key before
        if (null == keyObfuscationValue) {
            // first run the check of the key and store to map result
            Optional<Function<String, Object>> keyBasedObfuscation = shouldObfuscateKey(key);
            CheckedKeyObfuscationValue.CheckedKeyObfuscationValueBuilder checkedKeyObfuscationValueBuilder = CheckedKeyObfuscationValue.builder();

            // if function is present, then we know obfuscation is needed, and we save both boolean and function to the map
            keyBasedObfuscation.ifPresent(stringObjectFunction -> checkedKeyObfuscationValueBuilder.shouldObfuscate(true)
                    .obfuscationFunction(stringObjectFunction));
            CheckedKeyObfuscationValue cacheValue = checkedKeyObfuscationValueBuilder.build();
            checkedKeysMap.put(key, cacheValue);

            // return resolved function if key obfuscation
            if (cacheValue.shouldObfuscate) {
                return keyBasedObfuscation;
            }
        }

        // at the end check only data obfuscation
        return shouldObfuscateData(value);
    }

    private Optional<Function<String, Object>> shouldObfuscateKey(String key) {
        return patternEntries.stream()
                .filter(p -> p.matchesKey(key))
                .findFirst()
                .map(PatternEntry::getObfuscationFunction);
    }

    private Optional<Function<String, Object>> shouldObfuscateData(String data) {
        return patternEntries.stream()
                .filter(p -> p.matchesData(data))
                .findFirst()
                .map(PatternEntry::getObfuscationFunction);
    }

    /**
     * Used in the cache as value
     */
    @Builder
    @Value
    private static class CheckedKeyObfuscationValue {

        /**
         * If key should be obfuscated
         */
        boolean shouldObfuscate;

        /**
         * What's the obfuscation function
         */
        Function<String, Object> obfuscationFunction;

    }

    /**
     * Also see {@link ObfuscationPattern}
     */
    @Builder
    @Value
    public static class PatternEntry {

        /**
         * Pattern to be used when checking keys/values for needed obfuscation.
         * Already includes case-(in)sensitivity
         */
        Pattern pattern;

        /**
         * Replace regex to use when obfuscating value. Can be <code>null</code>
         */
        Pattern replaceRegex;

        /**
         * If key check should be performed
         *
         * @see #matchesKey(String)
         */
        boolean checkKey;

        /**
         * If data check should be performed
         *
         * @see #matchesData(String)
         */
        boolean checkData;

        boolean matchesKey(String key) {
            return checkKey && pattern.matcher(key).matches();
        }

        boolean matchesData(String data) {
            return checkData && pattern.matcher(data).matches();
        }

        Function<String, Object> getObfuscationFunction() {
            if (replaceRegex == null) return DEFAULT_OBFUSCATION_FUNCTION;
            else return value -> replaceRegex.matcher(value).replaceAll("***");
        }
    }
}
