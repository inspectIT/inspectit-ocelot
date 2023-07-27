package rocks.inspectit.ocelot.core.instrumentation.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  DataStorage for all tags, that should be propagated to one browser
 *  Normally, there should be only one data storage per session
 */
public class BrowserPropagationDataStorage {

    private long latestTimestamp;
    private final ConcurrentMap<String, Object> propagationData;

    public BrowserPropagationDataStorage() {
        latestTimestamp = System.currentTimeMillis();
        propagationData = new ConcurrentHashMap<>();
    }

    public void writeData(Map<String, Object> newPropagationData) {
        propagationData.putAll(newPropagationData);
    }

    public Map<String, Object> readData() {
        return propagationData;
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
}
