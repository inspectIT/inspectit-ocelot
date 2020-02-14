package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import io.opencensus.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@AllArgsConstructor
public class PatternObfuscatory implements IObfuscatory {

    /**
     * Patterns that need to be checked.
     * //TODO split to key and data pattern collections?? Kill pattern entry and decrease iteration on the first key check
     */
    private final Collection<PatternEntry> patternEntries;

    /**
     * Map holding already checked keys as key and info if the data should be obfuscated or not.
     * // TODO transform to the limited cache??
     */
    private final ConcurrentHashMap<String, Boolean> checkedKeysMap = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSpanAttribute(Span span, String key, String value) {
        String obfuscatedValue = value;

        if (shouldObfuscate(key, value)) {
            // TODO variable replacement (but then the map shortcut will not work)
            obfuscatedValue = "***";
        }

        IObfuscatory.super.putSpanAttribute(span, key, obfuscatedValue);
    }

    private boolean shouldObfuscate(String key, String value) {
        // get info from the map
        Boolean keyObfuscationResult = checkedKeysMap.get(key);

        // if we know this should be obfuscated return immediately
        if (Boolean.TRUE.equals(keyObfuscationResult)) {
            return true;
        }

        // if we have no information about this key before
        if (null == keyObfuscationResult) {

            // first run the check of the key and store to map
            boolean keyBasedObfuscation = shouldObfuscateKey(key);
            checkedKeysMap.putIfAbsent(key, keyBasedObfuscation);

            // return true if key obfuscation
            if (keyBasedObfuscation) {
                return true;
            }
        }

        // at the end check only data obfuscation
        return shouldObfuscateData(value);
    }

    private boolean shouldObfuscateKey(String key) {
        return patternEntries.stream()
                .anyMatch(p -> p.matchesKey(key));
    }

    private boolean shouldObfuscateData(String data) {
        return patternEntries.stream()
                .anyMatch(p -> p.matchesData(data));
    }

    @Builder
    @Value
    public static final class PatternEntry {
        private final Pattern pattern;
        private final boolean checkKey;
        private final boolean checkData;

        boolean matchesKey(String key) {
            return checkKey && pattern.matcher(key).matches();
        }

        boolean matchesData(String data) {
            return checkData && pattern.matcher(data).matches();
        }

    }

}
