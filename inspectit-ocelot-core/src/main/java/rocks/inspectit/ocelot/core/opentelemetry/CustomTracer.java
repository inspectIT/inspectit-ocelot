package rocks.inspectit.ocelot.core.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.internal.AttributesMap;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.Builder;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.util.List;

/**
 * A custom {@link Tracer} that uses an underlying {@link SdkTracerProvider} that offers to set a {@link Sampler} and {@link Clock}
 * It autowires the {@link OpenTelemetryControllerImpl} the retrieve the {@link io.opentelemetry.sdk.trace.IdGenerator}, {@link io.opentelemetry.sdk.trace.SpanProcessor}, and {@link {@link io.opentelemetry.sdk.resources.Resource}.
 */
public class CustomTracer implements Tracer {

    /**
     * The custom {@link Clock} to use when building spans.
     * <b>IMPORTANT: </b> this does not take effect if the new span is a chield span, i.e., it has a parent {@link io.opentelemetry.api.trace.Span}, as the {@link io.opentelemetry.sdk.trace.SdkSpan#startSpan(SpanContext, String, InstrumentationScopeInfo, SpanKind, Span, Context, SpanLimits, SpanProcessor, Clock, Resource, AttributesMap, List, int, long)} overrides the clock by the parent span's clock.
     */
    private final Clock clock;

    private final Sampler sampler;

    // cannot use @AutoWired due to the @Builder pattern. Thus, get it from Instances.
    private final OpenTelemetryControllerImpl openTelemetryController = (OpenTelemetryControllerImpl) Instances.openTelemetryController;

    @VisibleForTesting

    final SdkTracerProvider tracerProvider;

    @Builder
    CustomTracer(Clock clock, Sampler sampler) {
        this.clock = clock;
        this.sampler = sampler;
        // if neither sampler nor clock are set, use the default tracer provider
        if (null == clock && null == sampler) {
            tracerProvider = openTelemetryController.getTracerProvider();
            return;
        }
        SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder()
                .setIdGenerator(openTelemetryController.getIdGenerator())
                .addSpanProcessor(openTelemetryController.getSpanProcessor())
                .setResource(openTelemetryController.getTracerProviderAttributes());
        if (null != this.sampler) {
            sdkTracerProviderBuilder.setSampler(this.sampler);
        }
        if (null != this.clock) {
            sdkTracerProviderBuilder.setClock(this.clock);
        }
        tracerProvider = sdkTracerProviderBuilder.build();
    }

    @Override
    public SpanBuilder spanBuilder(String spanName) {
        return tracerProvider.get(OpenTelemetryUtils.DEFAULT_INSTRUMENTATION_SCOPE_INFO, OpenTelemetryUtils.DEFAULT_INSTRUMENTATION_SCOPE_VERSION)
                .spanBuilder(spanName);
    }
}
