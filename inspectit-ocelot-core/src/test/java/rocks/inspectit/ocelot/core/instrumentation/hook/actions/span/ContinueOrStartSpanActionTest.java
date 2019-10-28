package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Sampler;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContinueOrStartSpanActionTest {

    @Nested
    public class ConfigureSampler {

        @Mock
        IHookAction.ExecutionContext context;

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
            VariableAccessor dynamicProbability = Mockito.mock(VariableAccessor.class);
            when(dynamicProbability.get(any())).thenReturn(0.42);

            Sampler result = ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityAccessor(dynamicProbability)
                    .build()
                    .getSampler(context);

            Object configuredProbability = ReflectionTestUtils.invokeMethod(result, "getProbability");
            assertThat((Double) configuredProbability).isEqualTo(0.42);
            verify(dynamicProbability).get(any());
            verifyNoMoreInteractions(context);
        }


        @Test
        void dynamicNullProbability() {
            VariableAccessor dynamicProbability = Mockito.mock(VariableAccessor.class);
            when(dynamicProbability.get(any())).thenReturn(null);

            Sampler result = ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityAccessor(dynamicProbability)
                    .build()
                    .getSampler(context);

            assertThat(result).isNull();
            verify(dynamicProbability).get(any());
            verifyNoMoreInteractions(context);
        }

    }
}
