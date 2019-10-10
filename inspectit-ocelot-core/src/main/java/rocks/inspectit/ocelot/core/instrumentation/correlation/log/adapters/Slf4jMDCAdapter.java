package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.lang.reflect.Method;

/**
 * Adapter for accessing the slf4j MDC class using reflection.
 * As Logback natively uses this MDC class it automatically includes logback support.
 */
@Slf4j
public class Slf4jMDCAdapter implements MDCAdapter {

    public static final String MDC_CLASS = "org.slf4j.MDC";

    private WeakMethodReference putMethod;
    private WeakMethodReference getMethod;
    private WeakMethodReference removeMethod;

    private Slf4jMDCAdapter(WeakMethodReference put, WeakMethodReference get, WeakMethodReference remove) {
        putMethod = put;
        getMethod = get;
        removeMethod = remove;
    }

    public static Slf4jMDCAdapter get(Class<?> mdcClazz) {
        try {
            WeakMethodReference put = WeakMethodReference.create(mdcClazz, "put", String.class, String.class);
            WeakMethodReference get = WeakMethodReference.create(mdcClazz, "get", String.class);
            WeakMethodReference remove = WeakMethodReference.create(mdcClazz, "remove", String.class);
            return new Slf4jMDCAdapter(put, get, remove);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("MDC class did not contain expected methods", e);
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
