package rocks.inspectit.ocelot.core.instrumentation.browser;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton storage for multiple {@link BrowserPropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote browser
 */

@Slf4j
public class BrowserPropagationSessionStorage {

    private static BrowserPropagationSessionStorage instance;
    private final ConcurrentMap<String, BrowserPropagationDataStorage> dataStorages;

    private BrowserPropagationSessionStorage() {
        dataStorages = new ConcurrentHashMap<>();
    }

    public static synchronized BrowserPropagationSessionStorage getInstance() {
        if(instance == null) instance = new BrowserPropagationSessionStorage();
        return instance;
    }

    public BrowserPropagationDataStorage getOrCreateDataStorage(String sessionID) {
        BrowserPropagationDataStorage dataStorage = dataStorages.get(sessionID);
        if(dataStorage == null) dataStorages.put(sessionID, new BrowserPropagationDataStorage());
        return dataStorages.get(sessionID);
    }

    public BrowserPropagationDataStorage getDataStorage(String sessionID) {
        return dataStorages.get(sessionID);
    }

    public void clearDataStorages() {
        dataStorages.clear();
    }

    /**
     * Checks if data storages are expired and removes them
     * @param timeToLive How long should data be stored in seconds
     */
    public void cleanUpData(int timeToLive) {
        long currentTime = System.currentTimeMillis();
        dataStorages.forEach((id, storage) -> {
            long elapsedTime = storage.calculateElapsedTime(currentTime) / 1000;
            if(timeToLive < elapsedTime) dataStorages.remove(id);
        });
    }
}