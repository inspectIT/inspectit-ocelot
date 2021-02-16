package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.InjectionScope;

@Slf4j
@AllArgsConstructor
public abstract class DelegationMdcAccessor extends MdcAccessor {

    private final MdcAccessor mdcAccessor;

    public abstract boolean isEnabled(TraceIdMDCInjectionSettings settings);

    @Override
    public Object get(String key) {
        return mdcAccessor.get(key);
    }

    @Override
    public void put(String key, Object value) {
        mdcAccessor.put(key, value);
    }

    @Override
    public void remove(String key) {
        mdcAccessor.remove(key);
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
