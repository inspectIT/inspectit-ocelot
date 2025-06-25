package rocks.inspectit.ocelot.core.instrumentation.context.session;

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
public class PropagationDataStorage {

    // Default limit of OpenTelemetry is also 128
    private static final int TAG_LIMIT = 128;

    private static final int MAX_KEY_SIZE = 128;

    private static final int MAX_VALUE_SIZE = 2048;

    private long latestTimestamp;

    private final ConcurrentMap<String, Object> propagationData = new ConcurrentHashMap<>();

    // TODO We can remove this property, after browser-propagation was removed
    //  Then only the inspectIT context uses the storage, which already knows the propagation
    private PropagationMetaData propagation;

    PropagationDataStorage() {
        latestTimestamp = System.currentTimeMillis();
    }

    /**
     * Updates the propagation, if changed
     */
    public void setPropagation(PropagationMetaData propagation) {
        if (propagation != null && !propagation.equals(this.propagation)) this.propagation = propagation;
    }

    /**
     * Writes the provided data into the storage, if the storage is not exceeded.
     * Invalid data entries will not be written into the storage.
     *
     * @param newPropagationData the new data
     */
    public void writeData(Map<String, ?> newPropagationData) {
        Map <String, Object> validatedData = validateEntries(newPropagationData);

        if (exceedsTagLimit(validatedData)) {
            log.debug("Unable to write data: tag limit was exceeded");
            return;
        }

        updateTimestamp();
        propagationData.putAll(validatedData);
    }

    /**
     * @return a copy of the current propagation data
     */
    public Map<String, Object> readData() {
        updateTimestamp();
        return new HashMap<>(propagationData);
    }

    public int getStorageSize() {
        return propagationData.size();
    }

    private void updateTimestamp() {
        latestTimestamp = System.currentTimeMillis();
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
            if (validateEntry(k,v)) validatedData.put(k,v);
            else log.debug("Invalid data entry {} will not be stored", k);
        });
        return validatedData;
    }

    private boolean validateEntry(String key, Object value) {
        return key.length() <= MAX_KEY_SIZE &&
                shouldBeStored(key) &&
                value instanceof String &&
                ((String) value).length() <= MAX_VALUE_SIZE;
    }

    /**
     * Only if session-storage (or browser-propagation) is enabled for this key, it should be stored.
     *
     * @param key the key name
     * @return true, if this key should be stored
     */
    private boolean shouldBeStored(String key) {
        if (propagation != null) return propagation.isStoredForSession(key) || propagation.isPropagatedWithBrowser(key);
        else return false;
    }
}
