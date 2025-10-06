package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import lombok.Value;
import rocks.inspectit.ocelot.core.privacy.obfuscation.IObfuscatory;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

/**
 * The {@link IObfuscatory} that monitors duration of call to a delegating obfuscatory.
 */
@Value
public class SelfMonitoringDelegatingObfuscatory implements IObfuscatory {

    SelfMonitoringService selfMonitoringService;

    /**
     * {@link IObfuscatory} to delegate to
     */
    IObfuscatory delegatingObfuscatory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSpanAttribute(Span span, String key, Object value) {
        try (Scope scope = selfMonitoringService.withDurationSelfMonitoring(delegatingObfuscatory.getClass()
                .getSimpleName())) {
            delegatingObfuscatory.putSpanAttribute(span, key, value);
        }
    }
}
