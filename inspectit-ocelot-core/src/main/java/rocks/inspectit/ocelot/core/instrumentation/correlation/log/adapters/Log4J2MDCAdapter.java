package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Provides access to Log4j2s ThreadContext.
 */
@Slf4j
public class Log4J2MDCAdapter implements MDCAdapter {

    /**
     * The name of the MDC class of Log4j2.
     */
    public static final String THREAD_CONTEXT_CLASS = "org.apache.logging.log4j.ThreadContext";

    /**
     * Reference to the org.apache.logging.log4j.ThreadContext.put(key,value) method.
     */
    private WeakMethodReference putMethod;

    /**
     * Reference to the org.apache.logging.log4j.ThreadContext.get(key) method.
     */
    private WeakMethodReference getMethod;

    /**
     * Reference to the org.apache.logging.log4j.ThreadContext.remove(key) method.
     */
    private WeakMethodReference removeMethod;

    private Log4J2MDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        putMethod = put;
        getMethod = get;
        removeMethod = remove;
    }

    /**
     * Creates an Adapater given a ThreadContext class.
     *
     * @param mdcClazz the org.apache.logging.log4j.ThreadContext class
     * @return and adapter for setting values on the given thread context.
     */
    public static Log4J2MDCAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Log4J2MDCAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("ThreadContext class did not contain expected methods", e);
        }
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
