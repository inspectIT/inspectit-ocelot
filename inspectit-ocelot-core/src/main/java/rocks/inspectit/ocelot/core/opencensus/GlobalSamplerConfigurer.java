package rocks.inspectit.ocelot.core.opencensus;

import io.opencensus.trace.Sampler;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;

/**
 * Configures the global OpenCensus sample with the probability configured via Ocelot.
 */
@Component
public class GlobalSamplerConfigurer {

    @Autowired
    InspectitEnvironment env;

    @PostConstruct
    @EventListener(InspectitConfigChangedEvent.class)
    void configureGlobalSampler() {
        TracingSettings settings = env.getCurrentConfig().getTracing();

        double probability = settings.getSampleProbability();
        Sampler sampler = Samplers.probabilitySampler(probability);

        TraceParams activeParams = Tracing.getTraceConfig().getActiveTraceParams();
        TraceParams updatedParams = activeParams.toBuilder()
                .setSampler(sampler)
                .build();
        Tracing.getTraceConfig().updateActiveTraceParams(updatedParams);
    }
}
