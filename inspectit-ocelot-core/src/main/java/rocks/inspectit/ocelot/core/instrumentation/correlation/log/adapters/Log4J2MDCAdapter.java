package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Provides access to Log4j2s ThreadContext.
 */
@Slf4j
public class Log4J2MDCAdapter implements MDCAdapter {

    public static final String THREAD_CONTEXT_CLASS = "org.apache.logging.log4j.ThreadContext";

    private WeakMethodReference putMethod;
    private WeakMethodReference getMethod;
    private WeakMethodReference removeMethod;

    private Log4J2MDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        putMethod = put;
        getMethod = get;
        removeMethod = remove;
    }

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
    public Undo set(String key, String value) {
        Method put = putMethod.get();
        Method get = getMethod.get();
        Method remove = removeMethod.get();

        if (put == null || get == null || remove == null) {
            return () -> {
            }; //the MDC has been garbage collected
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
            return () -> {
            };
        }


    }
}
