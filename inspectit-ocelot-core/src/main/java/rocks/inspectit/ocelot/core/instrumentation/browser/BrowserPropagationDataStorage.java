package rocks.inspectit.ocelot.core.instrumentation.browser;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  DataStorage for all tags, that should be propagated to one browser
 *  Normally, there should be only one data storage per session
 */
@Slf4j
public class BrowserPropagationDataStorage {

    // Default limit of OpenTelemetry is also 128
    private static final int TAG_LIMIT = 128;

    private static final int MAX_KEY_SIZE = 128;

    private static final int MAX_VALUE_SIZE = 2048;

    private long latestTimestamp;

    private final ConcurrentMap<String, Object> propagationData = new ConcurrentHashMap<>();

    private PropagationMetaData propagation;

    BrowserPropagationDataStorage() {
        latestTimestamp = System.currentTimeMillis();
    }

    /**
     * Updates the propagation, if changed
     */
    public void setPropagation(PropagationMetaData propagation) {
        if (propagation != null && !propagation.equals(this.propagation)) this.propagation = propagation;
    }

    public void writeData(Map<String, ?> newPropagationData) {
        Map <String, Object> validatedData = validateEntries(newPropagationData);

        if(exceedsTagLimit(validatedData)) {
            log.debug("Unable to write data: tag limit was exceeded");
            return;
        }
        propagationData.putAll(validatedData);
    }

    public Map<String, Object> readData() {
        return new HashMap<>(propagationData);
    }

    public int getStorageSize() {
        return propagationData.size();
    }

    public void updateTimestamp(long newTimestamp) {
        latestTimestamp = newTimestamp;
    }

    /**
     * Calculates the elapsed time since latestTimestamp
     *
     * @param currentTime current time in milliseconds
     * @return Elapsed time since latestTimestamp in milliseconds
     */
    public long calculateElapsedTime(long currentTime) {
        return currentTime - latestTimestamp;
    }

    private boolean exceedsTagLimit(Map<String, ?> newPropagationData) {
        Set<String> keySet = new HashSet<>(propagationData.keySet());
        keySet.retainAll(newPropagationData.keySet());
        // Add size of both maps and subtract the common keys
        return propagationData.size() + newPropagationData.size() - keySet.size() > TAG_LIMIT;
    }

    private Map<String, Object> validateEntries(Map<String, ?> newPropagationData) {
        Map<String, Object> validatedData = new HashMap<>();
        newPropagationData.forEach((k,v) -> {
            if(validateEntry(k,v)) validatedData.put(k,v);
            else log.debug("Invalid data entry {} will not be stored", k);
        });
        return validatedData;
    }

    private boolean validateEntry(String key, Object value) {
        return key.length() <= MAX_KEY_SIZE &&
                isPropagated(key) &&
                value instanceof String &&
                ((String) value).length() <= MAX_VALUE_SIZE;
    }

    /**
     * Only if browser-propagation is enabled for this key, it should be stored.
     *
     * @param key the key name
     * @return true, if this key should be propagated
     */
    private boolean isPropagated(String key) {
        if(propagation != null) return propagation.isPropagatedWithBrowser(key);
        else return false;
    }
}
