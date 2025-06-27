package rocks.inspectit.ocelot.core.instrumentation.context.session;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  Data storage for all tags, that should be available for a specific amount of time.
 *  There should be only one data storage per session.
 */
@Slf4j
public class PropagationDataStorage {

    // Default limit of OpenTelemetry is also 128
    private static final int TAG_LIMIT = 128;

    private static final int MAX_KEY_SIZE = 128;

    private static final int MAX_VALUE_SIZE = 2048;

    /**
     * Last time, when data was updated
     */
    private long latestTimestamp;

    /**
     * The current propagation settings, to check which data should be stored
     */
    @Setter
    private PropagationMetaData propagation;

    private final ConcurrentMap<String, Object> propagationData = new ConcurrentHashMap<>();

    PropagationDataStorage(PropagationMetaData propagation) {
        this.latestTimestamp = System.currentTimeMillis();
        this.propagation = propagation;
    }

    /**
     * Writes the provided data into the storage, if the storage is not exceeded.
     * Invalid data entries will not be written into the storage.
     * Updates the data timestamp.
     *
     * @param newPropagationData the new data
     */
    public void writeData(Map<String, ?> newPropagationData) {
        Map <String, Object> validatedData = validateEntries(newPropagationData);

        if (!validatedData.isEmpty()) {
            if (exceedsTagLimit(validatedData)) {
                log.debug("Unable to write data: tag limit was exceeded");
            } else {
                updateTimestamp();
                propagationData.putAll(validatedData);
            }
        }
    }

    /**
     * @return a copy of all the propagation data
     */
    public Map<String, Object> readData() {
        return new HashMap<>(propagationData);
    }

    /**
     * @param key the data key
     * @return the specific entry for the provided key
     */
    public Object readData(String key) {
        return propagationData.get(key);
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
        newPropagationData.forEach((key,value) -> {
            if (isValidEntry(key, value)) validatedData.put(key, value);
            else log.debug("Invalid data entry {} will not be stored", key);
        });
        return validatedData;
    }

    /**
     * Keys and values have size restrictions.
     * Additionally, we only write data, which is configured to be stored in session.
     * We cannot store session ids (with key {@code remote_session_id}) themselves here,
     * because they should be provided via instrumentation actions.
     */
    private boolean isValidEntry(String key, Object value) {
        return key.length() <= MAX_KEY_SIZE &&
                shouldBeStored(key) &&
                !key.equals(InternalInspectitContext.REMOTE_SESSION_ID) &&
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
