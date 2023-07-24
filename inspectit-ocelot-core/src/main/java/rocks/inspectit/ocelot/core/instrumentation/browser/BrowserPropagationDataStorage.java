package rocks.inspectit.ocelot.core.instrumentation.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton DataStorage for all tags, that should be propagated to browser
 */
public class BrowserPropagationDataStorage {

    private static BrowserPropagationDataStorage instance;
    private final ConcurrentMap<String, Object> propagationData;

    private BrowserPropagationDataStorage() {
        propagationData = new ConcurrentHashMap<>();
    }

    public static synchronized BrowserPropagationDataStorage getInstance() {
        if(instance == null) instance = new BrowserPropagationDataStorage();
        return instance;
    }

    public void writeData(Map<String, Object> newPropagationData) {
        propagationData.putAll(newPropagationData);
    }

    public Map<String, Object> readData() {
        return propagationData;
    }
}
