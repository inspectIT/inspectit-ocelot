package rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics;

import io.opentelemetry.api.baggage.Baggage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MetricTagValueGuard;
import rocks.inspectit.ocelot.core.metrics.InstrumentManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricsRecorderTest extends SpringTestBase {

    @Mock
    InstrumentManager instrumentManager;

    @Mock
    IHookAction.ExecutionContext executionContext;

    @Spy
    @InjectMocks
    MetricTagValueGuard tagValueGuard;

    @Mock
    InspectitContextImpl inspectitContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment environment;

    @BeforeEach
    void setupMock() {
        when(executionContext.getInspectitContext()).thenReturn(inspectitContext);
    }

    @Nested
    class Execute {

        @Test
        void verifyNullValueDataMetricIgnored() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(null);
            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            verify(instrumentManager, never()).tryRecordingMetric(eq("my_metric"), any(Number.class));
            verify(instrumentManager, never()).tryRecordingMetric(eq("my_metric"), any(Number.class), any());

            when(variableAccess.get(any())).thenReturn(100L);

            rec.execute(executionContext);

            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), any(Number.class), eq(Baggage.empty()));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(Baggage.empty()));
        }

        @Test
        void verifyInvalidDataTypeHandled() {
            VariableAccessor dataA = Mockito.mock(VariableAccessor.class);
            VariableAccessor dataB = Mockito.mock(VariableAccessor.class);
            when(dataA.get(any())).thenReturn(100.0);
            when(dataB.get(any())).thenReturn("notanumber");
            MetricAccessor metricAccessorA = new MetricAccessor("my_metric1", dataA, Collections.emptyMap(), Collections.emptyMap());
            MetricAccessor metricAccessorB = new MetricAccessor("my_metric2", dataB, Collections.emptyMap(), Collections.emptyMap());

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            verify(dataB).get(any());
            verify(instrumentManager, times(1)).tryRecordingMetric(any(String.class), any(Number.class), eq(Baggage.empty()));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric1"), eq((Number) 100.0d), eq(Baggage.empty()));

            rec.execute(executionContext);

            verify(dataB, times(2)).get(any());
            verify(instrumentManager, times(2)).tryRecordingMetric(any(String.class), any(Number.class), eq(Baggage.empty()));
            verify(instrumentManager, times(2)).tryRecordingMetric(eq("my_metric1"), eq((Number) 100.0d), eq(Baggage.empty()));
        }

        @Test
        void commonTagsIncluded() {
            when(inspectitContext.getData("common")).thenReturn("overwrite");

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            Baggage expected = Baggage.builder().put("common", "overwrite").build();
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(expected));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        void constantTags() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.singletonMap("constant", "attribute"), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            Baggage expected = Baggage.builder().put("constant", "attribute").build();
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), any(Number.class), eq(expected));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(expected));
        }

        @Test
        void dataTagsNotAvailable() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn(null);
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), any(Number.class), eq(Baggage.empty()));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(Baggage.empty()));
        }

        @Test
        void dataTags() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn("value");

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            Baggage expected = Baggage.builder().put("data", "value").build();
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), any(Number.class), eq(expected));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(expected));
        }

        @Test
        void multipleAccessorsMixedTags() {
            VariableAccessor mockAccessorA = mock(VariableAccessor.class);
            when(mockAccessorA.get(any())).thenReturn("data1");
            VariableAccessor mockAccessorB = mock(VariableAccessor.class);
            when(mockAccessorB.get(any())).thenReturn(12L);
            VariableAccessor mockAccessorC = mock(VariableAccessor.class);
            when(mockAccessorC.get(any())).thenReturn(Boolean.FALSE);
            VariableAccessor mockAccessorD = mock(VariableAccessor.class);
            when(mockAccessorD.get(any())).thenReturn(null);

            VariableAccessor dataA = Mockito.mock(VariableAccessor.class);
            VariableAccessor dataB = Mockito.mock(VariableAccessor.class);
            when(dataA.get(any())).thenReturn(100.0);
            when(dataB.get(any())).thenReturn(200.0);
            HashMap<String, VariableAccessor> dataTags1 = new HashMap<>();
            dataTags1.put("existing", mockAccessorA);
            dataTags1.put("not_existing", mockAccessorD);
            MetricAccessor metricAccessorA = new MetricAccessor("my_metric1", dataA, Collections.singletonMap("cA", "100"), dataTags1);
            HashMap<String, VariableAccessor> dataTags2 = new HashMap<>();
            dataTags2.put("existing1", mockAccessorB);
            dataTags2.put("existing2", mockAccessorC);
            MetricAccessor metricAccessorB = new MetricAccessor("my_metric2", dataB, Collections.singletonMap("cA", "200"), dataTags2);

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            InOrder inOrder = inOrder(instrumentManager);
            // first recording
            Baggage expected1 = Baggage.builder()
                    .put("cA", "100")
                    .put("existing", "data1")
                    .build();

            inOrder.verify(instrumentManager).tryRecordingMetric(eq("my_metric1"), eq((Number) 100.0d), eq(expected1));

            // second recording
            Baggage expected2 = Baggage.builder()
                    .put("cA", "200")
                    .put("existing1", "12")
                    .put("existing2", "false")
                    .build();

            inOrder.verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric2"), eq((Number) 200.0d), eq(expected2));

            // and no more
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        void dataOverwritesConstant() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn("value");

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.singletonMap("data", "constant"), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), tagValueGuard, instrumentManager);

            rec.execute(executionContext);

            Baggage expected = Baggage.builder().put("data", "value").build();
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), any(Number.class), eq(expected));
            verify(instrumentManager, times(1)).tryRecordingMetric(eq("my_metric"), eq((Number) 100L), eq(expected));
        }
    }
}
