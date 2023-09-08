package rocks.inspectit.ocelot.core.instrumentation.hook.actions.span;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.LogTraceCorrelator;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.config.model.tracing.SampleMode;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.StackTraceSampler;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.tags.CommonTagsToAttributesManager;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;

import java.util.HashMap;

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
            Sampler result = ContinueOrStartSpanAction.builder().build().getSampler(context);

            assertThat(result).isNull();
            verifyNoMoreInteractions(context);
        }

        @Test
        void staticSampler() {
            Sampler sampler = Sampler.traceIdRatioBased(0.5);

            Sampler result = ContinueOrStartSpanAction.builder().staticSampler(sampler).build().getSampler(context);

            assertThat(result).isSameAs(sampler);
            verifyNoMoreInteractions(context);
        }

        @Test
        void dynamicNonNullProbability() {
            VariableAccessor dynamicProbability = Mockito.mock(VariableAccessor.class);
            when(dynamicProbability.get(any())).thenReturn(0.42);

            Sampler result = ContinueOrStartSpanAction.builder()
                    .dynamicSampleProbabilityAccessor(dynamicProbability)
                    .sampleMode(SampleMode.TRACE_ID_RATIO_BASED)
                    .build()
                    .getSampler(context);

            assertThat(result).isEqualTo(Sampler.traceIdRatioBased(0.42));

            Object configuredProbability = ((Long) ReflectionTestUtils.invokeMethod(result, "getIdUpperBound")).doubleValue() / Long.MAX_VALUE;
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

    @Nested
    public class RemoteParentContext {

        @Mock
        IHookAction.ExecutionContext executionContext;
        @Mock
        StackTraceSampler stackTraceSampler;
        @Mock
        CommonTagsToAttributesManager commonTagsToAttributesManager;
        @Mock
        InspectitContextImpl inspectitContext;

        MethodHook hook;
        ContinueOrStartSpanAction action;

        @BeforeEach
        void setUp() {
            hook = MethodHook.builder().actionScopeFactory(mock(ActionScopeFactory.class)).build();

            action = ContinueOrStartSpanAction.builder()
                    .continueSpanCondition(x -> false)
                    .startSpanCondition(x -> true)
                    .stackTraceSampler(stackTraceSampler)
                    .commonTagsToAttributesManager(commonTagsToAttributesManager)
                    .build();
        }

        @Test
        void verifyGetRemoteParentContextCalled() {
            when(executionContext.getInspectitContext()).thenReturn(inspectitContext);
            when(executionContext.getHook()).thenReturn(hook);
            when(inspectitContext.getAndClearCurrentRemoteSpanContext()).thenReturn(null);

            action.execute(executionContext);

            verify(inspectitContext, times(1)).getRemoteParentContext();
        }

        @Test
        void verifyGetRemoteParentContextNotCalled() {
            when(executionContext.getInspectitContext()).thenReturn(inspectitContext);
            when(executionContext.getHook()).thenReturn(hook);
            when(inspectitContext.getAndClearCurrentRemoteSpanContext()).thenReturn(SpanContext.getInvalid());

            action.execute(executionContext);

            verify(inspectitContext, times(0)).getRemoteParentContext();
        }
    }
}
