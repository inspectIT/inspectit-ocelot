package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
public abstract class DelegationMdcAccessor {

    private final BiConsumer<String, Object> putMethod;

    private final Function<String, Object> getMethod;

    private final Consumer<String> removeMethod;

    public abstract boolean isEnabled(TraceIdMDCInjectionSettings settings);

    public Object get(String key) {
        return getMethod.apply(key);
    }

    public void put(String key, Object value) {
        putMethod.accept(key, value);
    }

    public void remove(String key) {
        removeMethod.accept(key);
    }

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
