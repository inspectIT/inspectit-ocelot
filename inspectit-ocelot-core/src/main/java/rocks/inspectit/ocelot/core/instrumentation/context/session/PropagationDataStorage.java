package rocks.inspectit.ocelot.core.instrumentation.context.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.time.Duration;
import java.util.*;

/**
 *  Data storage for all tags that should be available for a specific amount of time.
 */
@Slf4j
public class PropagationDataStorage {

    /**
     * The maximum amount of tags per storage. OpenTelemetry also uses a limit of 128.
     * Warning: the cache may evict entries before this limit is exceeded -- typically when the cache size is approaching the limit.
     */
    private static final int TAG_LIMIT = 128;

    private static final int MAX_KEY_SIZE = 128;

    private static final int MAX_VALUE_SIZE = 2048;

    /**
     * The current propagation settings, to check which data should be stored
     */
    @Setter
    private PropagationMetaData propagation;

    /**
     * The cached propagation data
     */
    private volatile Cache<String, Object> propagationData;

    PropagationDataStorage(PropagationMetaData propagation, Duration ttl) {
        this.propagation = propagation;
        this.propagationData = buildCache(ttl);
    }

    /**
     * In order to be able to reconfigure the time-to-live at runtime, we have to rebuild the cache.
     * The currently stored data will be copied to the new cache.
     * After that the current cache wil lbe completed invalidated.
     *
     * @param ttl the new time-to-live
     */
    public synchronized void reconfigure(Duration ttl) {
        Cache<String, Object> oldCache = this.propagationData;
        Cache<String, Object> newCache = buildCache(ttl);

        oldCache.asMap().forEach((key, value) -> {
            if (value != null) newCache.put(key, value);
        });
        oldCache.invalidateAll();
        oldCache.cleanUp();

        this.propagationData = newCache;
    }

    /**
     * @param ttl the expiration time for each entry
     * @return the newly build cache
     */
    private Cache<String, Object> buildCache(Duration ttl) {
        return CacheBuilder.newBuilder()
                .maximumSize(TAG_LIMIT)
                .expireAfterWrite(ttl)
                .build();
    }

    /**
     * Writes the provided data into the storage, if the storage is not exceeded.
     * Invalid data entries will not be written into the storage.
     *
     * @param newPropagationData the new data
     */
    public void writeData(Map<String, ?> newPropagationData) {
        Map <String, Object> validatedData = validateEntries(newPropagationData);

        if (!validatedData.isEmpty())
            propagationData.putAll(validatedData);
    }

    /**
     * @return a copy of all the propagation data
     */
    public Map<String, Object> readData() {
        return new HashMap<>(propagationData.asMap());
    }

    /**
     * @param key the data key
     * @return the specific entry for the provided key
     */
    public Object readData(String key) {
        return propagationData.getIfPresent(key);
    }

    /**
     * @return the current cache size
     */
    public long getSize() {
        return propagationData.size();
    }

    /**
     * Removes all expired data entries.
     */
    public void cleanUp() {
        propagationData.cleanUp();
    }

    /**
     * Validate each entry in the provided data
     *
     * @param newPropagationData the new data
     * @return the map of only validated data entries
     */
    private Map<String, Object> validateEntries(Map<String, ?> newPropagationData) {
        Map<String, Object> validatedData = new HashMap<>();
        newPropagationData.forEach((key,value) -> {
            if (isValidEntry(key, value))
                validatedData.put(key, value);
            else
                log.debug("Invalid data entry {} will not be stored", key);
        });
        return validatedData;
    }

    /**
     * Keys and values have size restrictions.
     * Additionally, we only write data, which is configured to be stored in session.
     * We cannot store session ids (with key {@code remote_session_id}) themselves here,
     * because they should be provided via instrumentation actions.
     */
    private boolean isValidEntry(String key, Object value) {
        return key.length() <= MAX_KEY_SIZE &&
                shouldBeStored(key) &&
                !key.equals(InternalInspectitContext.REMOTE_SESSION_ID) &&
                value instanceof String &&
                ((String) value).length() <= MAX_VALUE_SIZE;
    }

    /**
     * Only if session-storage is enabled for this key, it should be stored.
     *
     * @param key the key name
     * @return true, if this key should be stored
     */
    private boolean shouldBeStored(String key) {
        if (propagation != null) return propagation.isStoredForSession(key);
        else return false;
    }
}
