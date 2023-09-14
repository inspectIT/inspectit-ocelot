package rocks.inspectit.ocelot.core.instrumentation.browser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton storage for multiple {@link BrowserPropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote browser
 */
@Slf4j
public class BrowserPropagationSessionStorage {

    private static final int KEY_MIN_SIZE = 64;
    private static final int KEY_MAX_SIZE = 512;

    @Getter
    @Setter
    private int sessionLimit = 100;
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
        return dataStorages.computeIfAbsent(sessionID, key -> {
            if(!validateSessionIdLength(key)) {
                log.warn("Unable to create session: Invalid key length");
                return null;
            }
            if(dataStorages.size() >= sessionLimit) {
                log.warn("Unable to create session: Session limit exceeded");
                return null;
            }
            return new BrowserPropagationDataStorage();
        });
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

    private boolean validateSessionIdLength(String sessionID) {
        int keyLength = sessionID.length();
        return keyLength <= KEY_MAX_SIZE && keyLength >= KEY_MIN_SIZE;
    }
}
