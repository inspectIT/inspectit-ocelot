package rocks.inspectit.ocelot.core.instrumentation.browser;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  DataStorage for all tags, that should be propagated to one browser
 *  Normally, there should be only one data storage per session
 */
@Slf4j
public class BrowserPropagationDataStorage {

    // Default AttributeCountLimit of OpenTelemetry is 128
    private static final int ATTRIBUTE_COUNT_LIMIT = 128;
    private static final int MAX_KEY_SIZE = 128;
    private static final int MAX_VALUE_SIZE = 2048;
    private long latestTimestamp;
    private final ConcurrentMap<String, Object> propagationData;

    public BrowserPropagationDataStorage() {
        latestTimestamp = System.currentTimeMillis();
        propagationData = new ConcurrentHashMap<>();
    }

    public void writeData(Map<String, ?> newPropagationData) {
        Map <String, Object> validatedData = validateEntries(newPropagationData);

        if(!validateAttributeLength(validatedData)) {
            log.debug("Unable to write data: Data count limit was exceeded");
            return;
        }
        propagationData.putAll(validatedData);
    }

    public Map<String, Object> readData() {
        return new HashMap<>(propagationData);
    }

    public void updateTimestamp(long newTimestamp) {
        latestTimestamp = newTimestamp;
    }

    /**
     * Calculates the elapsed time since latestTimestamp
     * @param currentTime current time in milliseconds
     * @return Elapsed time since latestTimestamp in milliseconds
     */
    public long calculateElapsedTime(long currentTime) {
        return currentTime - latestTimestamp;
    }

    private boolean validateAttributeLength(Map<String, ?> newPropagationData) {
        Set<String> keySet = new HashSet<>(propagationData.keySet());
        keySet.retainAll(newPropagationData.keySet());
        //Add size of both maps and subtract the common keys
        return propagationData.size() + newPropagationData.size() - keySet.size() <= ATTRIBUTE_COUNT_LIMIT;
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
        return value instanceof String &&
                key.length() <= MAX_KEY_SIZE &&
                ((String) value).length() <= MAX_VALUE_SIZE;
    }
}
