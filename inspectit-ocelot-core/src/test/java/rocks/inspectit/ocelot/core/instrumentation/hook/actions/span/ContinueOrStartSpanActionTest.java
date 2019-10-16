package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Sampler;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContinueOrStartSpanActionTest {

    @Nested
    public class ConfigureSampler {

        @Mock
        InspectitContext context;

        @Test
        void noSampler() {
            Sampler result = ContinueOrStartSpanAction.builder()
                    .build()
                    .getSampler(context);

            assertThat(result).isNull();
            verifyZeroInteractions(context);
        }

        @Test
        void staticSampler() {
            Sampler sampler = Samplers.probabilitySampler(0.5);

            Sampler result = ContinueOrStartSpanAction.builder()
                    .staticSampler(sampler)
                    .build()
                    .getSampler(context);

            assertThat(result).isSameAs(sampler);
            verifyZeroInteractions(context);
        }


        @Test
        void dynamicNonNullProbability() {
            doReturn(0.42).when(context).getData("my_key");

            Sampler result = ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityKey("my_key")
                    .build()
                    .getSampler(context);

            Object configuredProbability = ReflectionTestUtils.invokeMethod(result, "getProbability");
            assertThat((Double) configuredProbability).isEqualTo(0.42);
            verify(context).getData("my_key");
            verifyNoMoreInteractions(context);
        }


        @Test
        void dynamicNullProbability() {
            doReturn(null).when(context).getData("my_key");

            Sampler result = ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityKey("my_key")
                    .build()
                    .getSampler(context);

            assertThat(result).isNull();
            verify(context).getData("my_key");
            verifyNoMoreInteractions(context);
        }

    }
}
