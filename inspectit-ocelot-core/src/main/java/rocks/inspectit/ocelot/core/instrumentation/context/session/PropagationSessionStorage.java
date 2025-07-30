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
import java.util.Set;
import java.util.concurrent.*;

/**
 * Singleton storage for multiple {@link PropagationDataStorage} objects,
 * which are referenced by a session-ID, normally provided by a remote service.
 */
@Slf4j
@Component
public class PropagationSessionStorage {

    private static final int KEY_MIN_SIZE = 16;

    private static final int KEY_MAX_SIZE = 512;

    /**
     * Delay for the cleaning scheduler.
     */
    private static final Duration FIXED_DELAY = Duration.ofSeconds(30);

    /**
     * The data storages for each session.
     */
    private final ConcurrentMap<String, PropagationDataStorage> dataStorages = new ConcurrentHashMap<>();

    /**
     * Sessions, which are marked for removal. Such sessions will be removed at  If a session reaches 0 data tags,
     * it will be marked for removal when calling {@link #clearDataStorages()}. When calling the method again,
     * every session inside this set will be fully removed, if it's still empty. <br>
     * This should prevent, freshly created data storages will be removed before any data could have been written.
     */
    private final Set<String> sessionsForRemoval = ConcurrentHashMap.newKeySet();

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

    /**
     * Data storages cache their entries for a specific amount of time (timeToLive).
     * The storages are cleaned up regularly to remove expired data.
     */
    @PostConstruct
    void initialize() {
        sessionLimit = env.getCurrentConfig().getInstrumentation().getSessions().getSessionLimit();
        timeToLive = env.getCurrentConfig().getInstrumentation().getSessions().getTimeToLive();

        executor.scheduleWithFixedDelay(this::cleanUpStorages, FIXED_DELAY.toMillis(), FIXED_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        sessionLimit = event.getNewConfig().getInstrumentation().getSessions().getSessionLimit();

        Duration newTimeToLive = event.getNewConfig().getInstrumentation().getSessions().getTimeToLive();
        if (newTimeToLive != null && !newTimeToLive.equals(timeToLive))
            updateTimeToLive(newTimeToLive);
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
            return new PropagationDataStorage(propagation, timeToLive);
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
     * Triggers the cleanup of each data storage.
     * If the storage is empty, the session will be marked for removal.
     * If the session is empty and already marked for removal, it will be completely removed.
     * If the session is not empty but marked for removal, it will be unmarked.
     */
    @VisibleForTesting
    void cleanUpStorages() {
        dataStorages.forEach((id, storage) -> {
            storage.cleanUp();

            if (storage.getSize() < 1) {
                if (sessionsForRemoval.contains(id)) {
                    dataStorages.remove(id);
                    sessionsForRemoval.remove(id);
                }
                else {
                    sessionsForRemoval.add(id);
                }
            }
            else sessionsForRemoval.remove(id);
        });
    }

    /**
     * Updates the ttl for all data storages.
     * @param ttl the new time-to-live
     */
    private void updateTimeToLive(Duration ttl) {
        this.timeToLive = ttl;
        dataStorages.forEach((id, storage) -> storage.reconfigure(timeToLive));
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
     * Helper method for testing.
     */
    public void clearDataStorages() {
        dataStorages.clear();
    }
}
