package rocks.inspectit.ocelot.core.instrumentation.browser;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton storage for multiple {@link BrowserPropagationDataStorage}-objects,
 * which are referenced by a session-ID, normally provided by a remote browser
 */

@Component
@EnableScheduling
@Slf4j
public class BrowserPropagationSessionStorage {

    private static BrowserPropagationSessionStorage instance;
    private final ConcurrentMap<String, BrowserPropagationDataStorage> dataStorages;
    @Setter
    private int timeToLive = 300000;

    private BrowserPropagationSessionStorage() {
        dataStorages = new ConcurrentHashMap<>();
    }

    public static synchronized BrowserPropagationSessionStorage getInstance() {
        if(instance == null) instance = new BrowserPropagationSessionStorage();
        return instance;
    }

    public BrowserPropagationDataStorage getDataOrCreateStorage(String sessionID) {
        BrowserPropagationDataStorage dataStorage = dataStorages.get(sessionID);
        if(dataStorage == null) dataStorages.put(sessionID, new BrowserPropagationDataStorage());
        return dataStorages.get(sessionID);
    }

    public BrowserPropagationDataStorage getDataStorage(String sessionID) {
        return dataStorages.get(sessionID);
    }

    public void clearStorages() {
        dataStorages.clear();
    }

    @Scheduled(fixedDelay = 5000) //10000
    private void checkLiveTime() {
        long currentTime = System.currentTimeMillis();
        log.info("SCHEDULER WORKING");
        dataStorages.forEach((id, storage) -> {
            log.info("Active Session: " + id + "\n with size: " + storage.readData().size());
            int elapsedTime = storage.calculateElapsedTime(currentTime);
            if(timeToLive < elapsedTime) {
                dataStorages.remove(id);
                log.info("Session removed: " + id);
            }
        });
    }
}
