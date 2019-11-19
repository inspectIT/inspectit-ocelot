package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopTraceIdInjector;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdAutoInjectionSettings;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;

/**
 * This component handles the injection of the {@link rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector} instance
 * into the {@link Instances} class of the bootstrap classloader.
 */
@Component
public class TraceIdInjectorActivator {

    @Autowired
    private InspectitEnvironment environment;

    /**
     * Adds a new instance of the {@link rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector} into {@link Instances}
     * if auto injection is enabled. Otherwise it will use the noop instance.
     */
    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    public void activateInjector() {
        TracingSettings tracingSettings = environment.getCurrentConfig().getTracing();
        TraceIdAutoInjectionSettings injectionSettings = tracingSettings.getLogCorrelation().getTraceIdAutoInjection();

        if (injectionSettings.isEnabled()) {
            String prefix = injectionSettings.getPrefix();
            String suffix = injectionSettings.getSuffix();
            Instances.traceIdInjector = new TraceIdInjectorImpl(prefix, suffix);
        } else {
            Instances.traceIdInjector = NoopTraceIdInjector.INSTANCE;
        }
    }
}
