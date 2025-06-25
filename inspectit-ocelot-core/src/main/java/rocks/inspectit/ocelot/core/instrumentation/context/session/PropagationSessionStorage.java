package rocks.inspectit.ocelot.core.instrumentation.context.session;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Singleton storage for multiple {@link PropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote browser
 */
@Slf4j
@Component
public class PropagationSessionStorage {

    private static final int KEY_MIN_SIZE = 16;

    private static final int KEY_MAX_SIZE = 512;

    /**
     * Delay for the cleaning scheduler
     */
    private static final Duration FIXED_DELAY = Duration.ofMillis(30);

    @Autowired
    private ScheduledExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    /**
     * The maximum amount of sessions
     */
    private int sessionLimit = 100;

    /**
     * Time to live for session data in seconds
     */
    private Duration timeToLive = Duration.ofMinutes(5);

    /**
     * Boolean, which helps to create error messages, if browser propagation is tried, but exporter is disabled
     */
    @Getter @Setter
    private boolean isExporterActive = false;

    private final ConcurrentMap<String, PropagationDataStorage> dataStorages = new ConcurrentHashMap<>();

    /**
     * Data storages are cached for a specific amount of time (timeToLive).
     * If the time expires, clean up the storage.
     */
    @PostConstruct
    void initialize() {
        sessionLimit = env.getCurrentConfig().getInstrumentation().getSessions().getSessionLimit();
        timeToLive = env.getCurrentConfig().getInstrumentation().getSessions().getTimeToLive();

        executor.scheduleWithFixedDelay(this::cleanUpData, FIXED_DELAY.toMillis(), FIXED_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        int newSessionLimit = event.getNewConfig().getInstrumentation().getSessions().getSessionLimit();
        if (newSessionLimit != sessionLimit) sessionLimit = newSessionLimit;

        Duration newTimeToLive = event.getNewConfig().getInstrumentation().getSessions().getTimeToLive();
        if (newTimeToLive != null && !newTimeToLive.equals(timeToLive)) timeToLive = newTimeToLive;
    }

    public PropagationDataStorage getOrCreateDataStorage(String sessionID, PropagationMetaData propagation) {
        return dataStorages.computeIfAbsent(sessionID, key -> {
            if(!validateSessionIdLength(key)) {
                log.debug("Unable to create session: Invalid key length");
                return null;
            }
            if(dataStorages.size() >= sessionLimit) {
                log.debug("Unable to create session: Session limit exceeded");
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
     * Checks if data storages are expired and removes them.
     * We use milliseconds for calculation.
     */
    public void cleanUpData() {
        long currentTime = System.currentTimeMillis();
        dataStorages.forEach((id, storage) -> {
            long elapsedTime = storage.calculateElapsedTime(currentTime);
            System.out.println("ELAPSED: " + elapsedTime);
            if(timeToLive.toMillis() < elapsedTime) {
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
