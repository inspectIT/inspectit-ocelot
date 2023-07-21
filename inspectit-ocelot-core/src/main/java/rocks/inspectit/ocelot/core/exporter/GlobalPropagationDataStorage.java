package rocks.inspectit.ocelot.core.exporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GlobalPropagationDataStorage {

    private static GlobalPropagationDataStorage instance;
    private ConcurrentMap<String, Object> propagationData;

    private GlobalPropagationDataStorage() {
        this.propagationData = new ConcurrentHashMap<>();
    }

    public static synchronized GlobalPropagationDataStorage getInstance() {
        if(instance == null) instance = new GlobalPropagationDataStorage();
        return instance;
    }

    public void writeData(Map<String, Object> newPropagationData) {
        this.propagationData.putAll(newPropagationData);
    }

    public Map<String, Object> readData() {
        return this.propagationData;
    }
}
