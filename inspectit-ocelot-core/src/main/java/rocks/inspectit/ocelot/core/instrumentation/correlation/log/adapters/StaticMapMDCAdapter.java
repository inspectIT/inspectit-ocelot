package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Implementation for all MDCs in form of a static Map which offer a put, get and remove method.
 */
@Slf4j
public class StaticMapMDCAdapter implements MDCAdapter {

    /**
     * Reference to the put(key,value) method.
     */
    private WeakMethodReference putMethod;

    /**
     * Reference to the get(key) method.
     */
    private WeakMethodReference getMethod;

    /**
     * Reference to the remove(key) method.
     */
    private WeakMethodReference removeMethod;

    /**
     * Constructor.
     *
     * @param put    the static put(key, value) method of the MDC
     * @param get    the static get(key) method of the MDC
     * @param remove the static remove(key) method of the MDC
     */
    protected StaticMapMDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        putMethod = put;
        getMethod = get;
        removeMethod = remove;
    }

    @Override
    public MDCAccess.Undo set(String key, String value) {
        Method put = putMethod.get();
        Method get = getMethod.get();
        Method remove = removeMethod.get();

        if (put == null || get == null || remove == null) {
            return MDCAccess.Undo.NOOP; //the MDC has been garbage collected
        }

        try {

            Object previous = get.invoke(null, key);
            if (value != null) {
                put.invoke(null, key, value);
            } else {
                remove.invoke(null, key);
            }

            return () -> {
                try {
                    if (previous != null) {
                        put.invoke(null, key, previous);
                    } else {
                        remove.invoke(null, key);
                    }
                } catch (Throwable e) {
                    log.error("Could not reset MDC", e);
                }
            };
        } catch (Throwable e) {
            log.error("Could not write to MDC", e);
            return MDCAccess.Undo.NOOP;
        }
    }
}
