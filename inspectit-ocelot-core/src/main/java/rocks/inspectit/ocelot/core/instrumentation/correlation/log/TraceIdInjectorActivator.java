package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopTraceIdInjector;
import rocks.inspectit.ocelot.config.model.instrumentation.experimental.ExperimentalSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.experimental.TraceIdAutoInjectionSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;

@Component
public class TraceIdInjectorActivator {

    @Autowired
    private InspectitEnvironment environment;

    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    public void activateInjector() {
        ExperimentalSettings experimentalSettings = environment.getCurrentConfig().getInstrumentation().getExperimental();
        TraceIdAutoInjectionSettings injectionSettings = experimentalSettings.getTraceIdAutoInjectionSettings();

        if (injectionSettings.isEnabled()) {
            String prefix = injectionSettings.getPrefix();
            String suffix = injectionSettings.getSuffix();
            Instances.traceIdInjector = new TraceIdInjectorImpl(prefix, suffix);
        } else {
            Instances.traceIdInjector = NoopTraceIdInjector.INSTANCE;
        }
    }
}
