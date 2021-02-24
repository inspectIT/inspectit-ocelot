package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstract accessor for logging MDCs. Implementations of this class will be used to interact with a specific MDC, e.g.
 * for injecting values into it.
 */
@Slf4j
@AllArgsConstructor
public abstract class MdcAccessor {

    /**
     * The MDC class which is accessed by this accessor.
     */
    @Getter
    private final WeakReference<Class<?>> targetMdcClass;

    /**
     * Consumer for putting elements into the target MDC.
     */
    private final BiConsumer<String, Object> putMethod;

    /**
     * Function for getting elements of the MDC.
     */
    private final Function<String, Object> getMethod;

    /**
     * Consumer for removing elements from the MDC.
     */
    private final Consumer<String> removeMethod;

    /**
     * @return Returns whether the accessor should be enabled or not based on the given settings.
     */
    public abstract boolean isEnabled(TraceIdMDCInjectionSettings settings);

    /**
     * Returns the object which is stored in the target MDC under the specified key.
     *
     * @param key the key to use
     * @return the object associated with the given key
     */
    @VisibleForTesting
    Object get(String key) {
        return getMethod.apply(key);
    }

    /**
     * Injects the given value under the given key into the target MDC. Please note that existing values may be overwritten.
     *
     * @param key   the key to use
     * @param value the value to inject
     */
    @VisibleForTesting
    void put(String key, Object value) {
        putMethod.accept(key, value);
    }

    /**
     * Removes the value which is associated with the given key from the target MDC.
     *
     * @param key the key of the value to remove
     */
    @VisibleForTesting
    void remove(String key) {
        removeMethod.accept(key);
    }

    /**
     * Injects a given value under the given key into the target MDC. The current value which is stored under the given
     * key will be restored once the {@link InjectionScope} is closed.
     *
     * @param key   the key to use
     * @param value the value to inject
     * @return an {@link InjectionScope} to revert the injection and restore the MDC's initial state
     */
    public InjectionScope inject(String key, String value) {
        try {
            // store previous value - we will restore it once the scope is closed
            Object previous = get(key);

            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }

            return () -> {
                try {
                    if (previous != null) {
                        put(key, previous);
                    } else {
                        remove(key);
                    }
                } catch (Throwable e) {
                    log.error("Could not restore previous MDC value.", e);
                }
            };
        } catch (Throwable e) {
            log.error("Could not write to MDC.", e);
            return InjectionScope.NOOP;
        }
    }
}
