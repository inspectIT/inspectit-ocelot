package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasureTagValueGuard;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricsRecorderTest {

    @Mock
    CommonTagsManager commonTagsManager;

    @Mock
    MeasuresAndViewsManager metricsManager;

    @Mock
    IHookAction.ExecutionContext executionContext;

    @Spy
    @InjectMocks
    MeasureTagValueGuard tagValueGuard;

    @Mock
    InspectitContextImpl inspectitContext;

    @BeforeEach
    void setupMock() {
        when(commonTagsManager.getCommonTagKeys()).thenReturn(Collections.emptyList());
        when(executionContext.getInspectitContext()).thenReturn(inspectitContext);
       

    }

    @Nested
    class Execute {

        @Test
        void verifyNullValueDataMetricIgnored() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(null);
            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            verify(metricsManager, never()).tryRecordingMeasurement(eq("my_metric"), any(Number.class));
            verify(metricsManager, never()).tryRecordingMeasurement(eq("my_metric"), any(Number.class), any());

            when(variableAccess.get(any())).thenReturn(100L);

            rec.execute(executionContext);

            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), any(Number.class), eq(Tags.getTagger()
                    .empty()));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(Tags.getTagger()
                    .empty()));
        }

        @Test
        void verifyInvalidDataTypeHandled() {
            VariableAccessor dataA = Mockito.mock(VariableAccessor.class);
            VariableAccessor dataB = Mockito.mock(VariableAccessor.class);
            when(dataA.get(any())).thenReturn(100.0);
            when(dataB.get(any())).thenReturn("notanumber");
            MetricAccessor metricAccessorA = new MetricAccessor("my_metric1", dataA, Collections.emptyMap(), Collections.emptyMap());
            MetricAccessor metricAccessorB = new MetricAccessor("my_metric2", dataB, Collections.emptyMap(), Collections.emptyMap());

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            verify(dataB).get(any());
            verify(metricsManager, times(1)).tryRecordingMeasurement(any(String.class), any(Number.class), eq(Tags.getTagger()
                    .empty()));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric1"), eq((Number) 100.0d), eq(Tags.getTagger()
                    .empty()));

            rec.execute(executionContext);

            verify(dataB, times(2)).get(any());
            verify(metricsManager, times(2)).tryRecordingMeasurement(any(String.class), any(Number.class), eq(Tags.getTagger()
                    .empty()));
            verify(metricsManager, times(2)).tryRecordingMeasurement(eq("my_metric1"), eq((Number) 100.0d), eq(Tags.getTagger()
                    .empty()));
        }

        @Test
        void commonTagsIncluded() {
            when(inspectitContext.getData("common")).thenReturn("overwrite");
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Collections.singletonList(TagKey.create("common")));

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("common"), TagValue.create("overwrite"))
                    .build();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(expected));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        void constantTags() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.singletonMap("constant", "tag"), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("constant"), TagValue.create("tag"))
                    .build();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), any(Number.class), eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(expected));
        }

        @Test
        void dataTagsNotAvailable() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn(null);
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().empty();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), any(Number.class), eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(expected));
        }

        @Test
        void dataTags() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn("value");

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("data"), TagValue.create("value"))
                    .build();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), any(Number.class), eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(expected));
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

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            InOrder inOrder = inOrder(metricsManager);
            // first recording
            TagContext expected1 = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("cA"), TagValue.create("100"))
                    .putLocal(TagKey.create("existing"), TagValue.create("data1"))
                    .build();
            inOrder.verify(metricsManager)
                    .tryRecordingMeasurement(eq("my_metric1"), eq((Number) 100.0d), eq(expected1));
            // second recording
            TagContext expected2 = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("cA"), TagValue.create("200"))
                    .putLocal(TagKey.create("existing1"), TagValue.create("12"))
                    .putLocal(TagKey.create("existing2"), TagValue.create("false"))
                    .build();
            inOrder.verify(metricsManager, times(1))
                    .tryRecordingMeasurement(eq("my_metric2"), eq((Number) 200.0d), eq(expected2));
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
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, tagValueGuard);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger()
                    .emptyBuilder()
                    .putLocal(TagKey.create("data"), TagValue.create("value"))
                    .build();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), any(Number.class), eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), eq((Number) 100L), eq(expected));
        }
    }
}
