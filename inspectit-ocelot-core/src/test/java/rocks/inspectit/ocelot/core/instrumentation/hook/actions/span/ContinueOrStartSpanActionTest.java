package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opencensus.trace.Sampler;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContinueOrStartSpanActionTest {

    @Nested
    public class ConfigureSampler {

        @Mock
        SpanBuilder spanBuilder;

        @Mock
        InspectitContext context;

        @Test
        void noSampler() {
            Sampler sampler = Samplers.probabilitySampler(0.5);

            ContinueOrStartSpanAction.builder()
                    .build()
                    .configureSampler(spanBuilder, context);

            verifyZeroInteractions(spanBuilder);
            verifyZeroInteractions(context);
        }

        @Test
        void staticSampler() {
            Sampler sampler = Samplers.probabilitySampler(0.5);

            ContinueOrStartSpanAction.builder()
                    .staticSampler(sampler)
                    .build()
                    .configureSampler(spanBuilder, context);

            verify(spanBuilder).setSampler(same(sampler));
            verifyNoMoreInteractions(spanBuilder);
            verifyZeroInteractions(context);
        }


        @Test
        void dynamicNonNullProbability() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            doReturn(0.42).when(context).getData("my_key");

            ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityKey("my_key")
                    .build()
                    .configureSampler(spanBuilder, context);

            ArgumentCaptor<Sampler> sampler = ArgumentCaptor.forClass(Sampler.class);
            verify(spanBuilder).setSampler(sampler.capture());

            assertThat((Double) ReflectionTestUtils.invokeMethod(sampler.getValue(), "getProbability"))
                    .isEqualTo(0.42);
            verify(context).getData("my_key");
            verifyNoMoreInteractions(spanBuilder);
            verifyNoMoreInteractions(context);
        }


        @Test
        void dynamicNullProbability() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            doReturn(null).when(context).getData("my_key");

            ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityKey("my_key")
                    .build()
                    .configureSampler(spanBuilder, context);

            verify(context).getData("my_key");
            verifyNoMoreInteractions(spanBuilder);
            verifyNoMoreInteractions(context);
        }

    }
}
