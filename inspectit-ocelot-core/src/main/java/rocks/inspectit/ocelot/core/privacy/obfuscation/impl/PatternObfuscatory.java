package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opencensus.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

@AllArgsConstructor
public class PatternObfuscatory implements IObfuscatory {

    private static final Function<String, String> DEFAULT_OBFUSCATION_FUNCTION = (v) -> "***";

    /**
     * Patterns that need to be checked.
     */
    private final Collection<PatternEntry> patternEntries;

    /**
     * Map holding already checked keys as key and info if the data should be obfuscated or not.
     */
    private final Cache<String, CheckedKeyObfuscationValue> checkedKeysMap = CacheBuilder.newBuilder().maximumSize(1000).build();

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSpanAttribute(Span span, String key, String value) {
        String obfuscatedValue = shouldObfuscate(key, value)
                .map(function -> function.apply(value))
                .orElse(value);

        IObfuscatory.super.putSpanAttribute(span, key, obfuscatedValue);
    }

    private Optional<Function<String, String>> shouldObfuscate(String key, String value) {
        // get info from the map
        CheckedKeyObfuscationValue keyObfuscationValue = checkedKeysMap.getIfPresent(key);

        // if we know this should be obfuscated return immediately with function that was saved
        if (null != keyObfuscationValue && keyObfuscationValue.shouldObfuscate) {
            return Optional.of(keyObfuscationValue.obfuscationFunction);
        }

        // if we have no information about this key before
        if (null == keyObfuscationValue) {
            // first run the check of the key and store to map result
            Optional<Function<String, String>> keyBasedObfuscation = shouldObfuscateKey(key);
            CheckedKeyObfuscationValue.CheckedKeyObfuscationValueBuilder checkedKeyObfuscationValueBuilder = CheckedKeyObfuscationValue.builder();

            // if function is present, then we know obfuscation is needed and we save both boolean and function to the map
            keyBasedObfuscation.ifPresent(stringStringFunction -> checkedKeyObfuscationValueBuilder.shouldObfuscate(true).obfuscationFunction(stringStringFunction));
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

    private Optional<Function<String, String>> shouldObfuscateKey(String key) {
        return patternEntries.stream()
                .filter(p -> p.matchesKey(key))
                .findFirst()
                .map(PatternEntry::getObfuscationFunction);
    }

    private Optional<Function<String, String>> shouldObfuscateData(String data) {
        return patternEntries.stream()
                .filter(p -> p.matchesData(data))
                .findFirst()
                .map(PatternEntry::getObfuscationFunction);
    }

    /**
     * Used in the cache as value.
     */
    @Builder
    @Value
    private static final class CheckedKeyObfuscationValue {

        /**
         * If key should be obfuscated
         */
        private final boolean shouldObfuscate;

        /**
         * What's the obfuscation function.
         */
        private final Function<String, String> obfuscationFunction;

    }

    @Builder
    @Value
    public static final class PatternEntry {

        /**
         * Pattern to be used when checking keys/values for needed obfuscation.
         */
        private final Pattern pattern;

        /**
         * Replace regex to use when obfuscating value. Can be <code>null</code>.
         */
        private final String replaceRegex;

        /**
         * If key check should be performed.
         *
         * @see #matchesKey(String)
         */
        private final boolean checkKey;

        /**
         * If data check should be performed.
         *
         * @see #matchesData(String)
         */
        private final boolean checkData;

        boolean matchesKey(String key) {
            return checkKey && pattern.matcher(key).matches();
        }

        boolean matchesData(String data) {
            return checkData && pattern.matcher(data).matches();
        }

        Function<String, String> getObfuscationFunction() {
            return Optional.ofNullable(replaceRegex)
                    .map(regex -> (Function<String, String>) v -> v.replaceAll(regex, "***"))
                    .orElse(DEFAULT_OBFUSCATION_FUNCTION);
        }

    }

}
