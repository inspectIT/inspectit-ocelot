package rocks.inspectit.ocelot.core.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.Builder;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A custom {@link Tracer} that uses an underlying {@link SdkTracerProvider} that offers to set a {@link Sampler}.
 * It autowires the {@link OpenTelemetryControllerImpl} to retrieve the {@link io.opentelemetry.sdk.trace.IdGenerator}, {@link io.opentelemetry.sdk.trace.SpanProcessor}, and {@link {@link io.opentelemetry.sdk.resources.Resource}.
 */
public class CustomTracer implements Tracer {

    // cannot use @AutoWired due to the @Builder pattern. Thus, get it from Instances.
    private final OpenTelemetryControllerImpl openTelemetryController = (OpenTelemetryControllerImpl) Instances.openTelemetryController;

    @VisibleForTesting

    final SdkTracerProvider tracerProvider;

    static ConcurrentHashMap<Sampler, SdkTracerProvider> tracerProviderCache = new ConcurrentHashMap<>();

    @Builder
    CustomTracer(Sampler sampler) {
        // if neither sampler nor clock are set, use the default tracer provider
        if (null == sampler) {
            tracerProvider = openTelemetryController.getTracerProvider();
            return;
        }
        tracerProvider = tracerProviderCache.computeIfAbsent(sampler, samp -> SdkTracerProvider.builder()
                .setIdGenerator(openTelemetryController.getIdGenerator())
                .addSpanProcessor(openTelemetryController.getSpanProcessor())
                .setResource(openTelemetryController.getTracerProviderAttributes())
                .setSampler(sampler)
                .build());
    }

    @Override
    public SpanBuilder spanBuilder(String spanName) {
        return tracerProvider.get(OpenTelemetryUtils.DEFAULT_INSTRUMENTATION_SCOPE_INFO, OpenTelemetryUtils.DEFAULT_INSTRUMENTATION_SCOPE_VERSION)
                .spanBuilder(spanName);
    }
}
