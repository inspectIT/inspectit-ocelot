package rocks.inspectit.ocelot.core.instrumentation.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;

/**
 * This component only exists for listening on configuration changes and correctly setting the propagation format
 * in the {@link ContextPropagationUtil}.
 */
@Component
public class PropagationFormatManager {

    @Autowired
    private InspectitEnvironment env;

    @PostConstruct
    private void postConstruct() {
        InspectitConfig currentConfig = env.getCurrentConfig();
        PropagationFormat propagationFormat = currentConfig.getTracing().getPropagationFormat();
        ContextPropagationUtil.setPropagationFormat(propagationFormat);
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent event) {
        PropagationFormat oldFormat = event.getOldConfig().getTracing().getPropagationFormat();
        PropagationFormat newFormat = event.getNewConfig().getTracing().getPropagationFormat();

        if (oldFormat != newFormat) {
            ContextPropagationUtil.setPropagationFormat(newFormat);
        }
    }

}
