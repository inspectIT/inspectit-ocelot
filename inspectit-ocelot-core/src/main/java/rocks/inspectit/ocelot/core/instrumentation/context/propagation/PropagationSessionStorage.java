package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton storage for multiple {@link PropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote browser
 */
@Slf4j
@Component
public class PropagationSessionStorage {

    private static final int KEY_MIN_SIZE = 16;

    private static final int KEY_MAX_SIZE = 512;

    @Getter @Setter
    private int sessionLimit = 100;

    /**
     * Boolean, which helps to create error messages, if browser propagation is tried, but exporter is disabled
     */
    @Getter @Setter
    private boolean isExporterActive = false;

    private final ConcurrentMap<String, PropagationDataStorage> dataStorages = new ConcurrentHashMap<>();

    public PropagationDataStorage getOrCreateDataStorage(String sessionID, PropagationMetaData propagation) {
        return dataStorages.computeIfAbsent(sessionID, key -> {
            if(!validateSessionIdLength(key)) {
                log.warn("Unable to create session: Invalid key length");
                return null;
            }
            if(dataStorages.size() >= sessionLimit) {
                log.warn("Unable to create session: Session limit exceeded");
                return null;
            }
            PropagationDataStorage dataStorage = new PropagationDataStorage();
            dataStorage.setPropagation(propagation);
            return dataStorage;
        });
    }

    public PropagationDataStorage getDataStorage(String sessionID) {
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
            if(timeToLive < elapsedTime) {
                dataStorages.remove(id);
                log.debug("Time to Live expired for the following session: {}", id);
            }
            else {
                int storageSize = storage.getStorageSize();
                log.debug("There are {} data entries stored in session: {}", storageSize, id);
            }
        });
    }

    private boolean validateSessionIdLength(String sessionID) {
        return sessionID != null &&
                sessionID.length() <= KEY_MAX_SIZE &&
                sessionID.length() >= KEY_MIN_SIZE;
    }
}
