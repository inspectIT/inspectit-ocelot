package rocks.inspectit.ocelot.core.instrumentation.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BrowserPropagationDataStorage {

    private static BrowserPropagationDataStorage instance;
    private ConcurrentMap<String, Object> propagationData;

    private BrowserPropagationDataStorage() {
        this.propagationData = new ConcurrentHashMap<>();
    }

    public static synchronized BrowserPropagationDataStorage getInstance() {
        if(instance == null) instance = new BrowserPropagationDataStorage();
        return instance;
    }

    public void writeData(Map<String, Object> newPropagationData) {
        this.propagationData.putAll(newPropagationData);
    }

    public Map<String, Object> readData() {
        return this.propagationData;
    }
}
