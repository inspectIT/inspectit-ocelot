package rocks.inspectit.ocelot.core.instrumentation.context.session;

import com.google.common.annotations.VisibleForTesting;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Singleton storage for multiple {@link PropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote service.
 * <p>
 * The idea is similar to the {@link rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments}, but here we are
 * independent of specific Java objects.
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
     * Current propagation format to check, which data should be stored in sessions.
     * Will be used for each data storage.
     */
    @Setter
    private PropagationMetaData propagation;

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

    @EventListener
    private void instrumentationConfigEventListener(InstrumentationConfigurationChangedEvent event) {
        PropagationMetaData newPropagation = event.getNewConfig().getPropagationMetaData();

        if (newPropagation != null && newPropagation != propagation)
            updatePropagation(newPropagation);
    }

    /**
     * Get a data storage for the provided session-id. If no storage exists, we will try to create a new one.
     * A storage cannot be created if the session-id is invalid or the session limit as been exceeded.
     *
     * @param sessionId the session id for the storage
     * @return the storage for the provided id or {@code null}
     */
    public PropagationDataStorage getOrCreateDataStorage(String sessionId) {
        return dataStorages.computeIfAbsent(sessionId, key -> {
            if(!isValidSessionId(key)) {
                log.debug("Unable to create session: Invalid key length");
                return null;
            }
            if(dataStorages.size() >= sessionLimit) {
                log.debug("Unable to create session: Session limit exceeded");
                return null;
            }
            return new PropagationDataStorage(propagation);
        });
    }

    /**
     * Get a data storage for the provided session-id. If no storage exists, return {@code null}.
     *
     * @param sessionId the session id for the storage
     * @return the storage for the provided id or {@code null}
     */
    public PropagationDataStorage getDataStorage(String sessionId) {
        return dataStorages.get(sessionId);
    }

    /**
     * Checks if data storages are expired and removes them.
     * We use milliseconds for calculation.
     */
    private void cleanUpData() {
        long currentTime = System.currentTimeMillis();
        dataStorages.forEach((id, storage) -> {
            long elapsedTime = storage.calculateElapsedTime(currentTime);
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

    /**
     * Updates the propagation settings for all data storages.
     */
    private void updatePropagation(PropagationMetaData propagation) {
        this.propagation = propagation;
        dataStorages.forEach((id, storage) -> storage.setPropagation(propagation));
    }

    /**
     * The session id must not be null and has restrictions for its length.
     *
     * @param sessionId the provided session id
     * @return true, if the session id is valid
     */
    private boolean isValidSessionId(String sessionId) {
        return sessionId != null &&
                sessionId.length() <= KEY_MAX_SIZE &&
                sessionId.length() >= KEY_MIN_SIZE;
    }

    /**
     * Helper method for testing
     */
    public void clearDataStorages() {
        dataStorages.clear();
    }
}
