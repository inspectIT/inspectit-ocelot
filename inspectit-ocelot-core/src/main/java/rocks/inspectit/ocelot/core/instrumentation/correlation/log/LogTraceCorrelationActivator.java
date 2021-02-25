package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;

/**
 * Enables / disables log-correlation by setting the {@link Instances#logTraceCorrelator}
 * to either a Noop or a functioning log-trace-correlator.
 */
@Component
public class LogTraceCorrelationActivator {

    @Autowired
    private InspectitEnvironment environment;

    @Autowired
    private LogTraceCorrelatorImpl correlatorImpl;

    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    public void update() {
        InspectitConfig configuration = environment.getCurrentConfig();
        TraceIdMDCInjectionSettings correlationSettings = configuration.getTracing().getLogCorrelation().getTraceIdMdcInjection();

        if (correlationSettings.isEnabled()) {
            correlatorImpl.setTraceIdKey(correlationSettings.getKey());
            Instances.logTraceCorrelator = correlatorImpl;
        } else {
            Instances.logTraceCorrelator = NoopLogTraceCorrelator.INSTANCE;
        }
    }

}
