package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.opentelemetry.trace.samplers.HybridParentTraceIdRatioBasedSampler;
import rocks.inspectit.ocelot.core.utils.ReflectionUtils;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class for {@link CustomTracer}
 */
public class CustomTracerIntTest extends SpringTestBase {

    private static final Class<?> TRACERSHAREDSTATE_CLASS;

    private static final Field SDKTRACERBUILDER_TRACERSHAREDSTATE;

    private static final Field TRACERSHAREDSTATE_CLOCK;

    private static final Field TRACERSHAREDSTATE_SAMPLER;

    @Autowired
    OpenTelemetryControllerImpl openTelemetryController;

    static {
        try {
            TRACERSHAREDSTATE_CLASS = Class.forName("io.opentelemetry.sdk.trace.TracerSharedState");
            SDKTRACERBUILDER_TRACERSHAREDSTATE = ReflectionUtils.getFieldAndMakeAccessible(SdkTracerProvider.class, "sharedState");
            TRACERSHAREDSTATE_CLOCK = ReflectionUtils.getFieldAndMakeAccessible(TRACERSHAREDSTATE_CLASS.getName(), "clock");
            TRACERSHAREDSTATE_SAMPLER = ReflectionUtils.getFieldAndMakeAccessible(TRACERSHAREDSTATE_CLASS.getName(), "sampler");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void customClock() throws IllegalAccessException {
        Clock dummyClock = new Clock() {
            @Override
            public long now() {
                return 1337;
            }

            @Override
            public long nanoTime() {
                return 47;
            }
        };
        CustomTracer customTracer = CustomTracer.builder().clock(dummyClock).build();
        assertThat(TRACERSHAREDSTATE_CLOCK.get(SDKTRACERBUILDER_TRACERSHAREDSTATE.get(customTracer.tracerProvider))).isEqualTo(dummyClock);
    }

    @Test
    void customSampler() throws IllegalAccessException {
        Sampler sampler = HybridParentTraceIdRatioBasedSampler.create(.42);
        CustomTracer customTracer = CustomTracer.builder().sampler(sampler).build();
        assertThat(TRACERSHAREDSTATE_SAMPLER.get(SDKTRACERBUILDER_TRACERSHAREDSTATE.get(customTracer.tracerProvider))).isEqualTo(sampler);
    }

    @Test
    void testDefault() {
        CustomTracer customTracer = CustomTracer.builder().build();
        assertThat(customTracer.tracerProvider).isEqualTo(openTelemetryController.getTracerProvider());
    }
}
