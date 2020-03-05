package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricsRecorderTest {

    @Mock
    CommonTagsManager commonTagsManager;

    @Mock
    MeasuresAndViewsManager metricsManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StatsRecorder statsRecorder;

    @Mock
    MeasureMap measureMap;

    @Mock
    IHookAction.ExecutionContext executionContext;

    @Mock
    InspectitContextImpl inspectitContext;

    @BeforeEach
    void setupMock() {
        when(commonTagsManager.getCommonTagKeys()).thenReturn(Collections.emptyList());
        when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
        when(executionContext.getInspectitContext()).thenReturn(inspectitContext);
    }

    @Nested
    class Execute {

        @Test
        void verifyNullValueDataMetricIgnored() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(null);
            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            verifyNoMoreInteractions(measureMap);
            verify(metricsManager, never()).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));

            when(variableAccess.get(any())).thenReturn(100L);

            rec.execute(executionContext);

            verify(measureMap, times(1)).record(eq(Tags.getTagger().empty()));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }

        @Test
        void verifyInvalidDataTypeHandled() {
            VariableAccessor dataA = Mockito.mock(VariableAccessor.class);
            VariableAccessor dataB = Mockito.mock(VariableAccessor.class);
            when(dataA.get(any())).thenReturn(100.0);
            when(dataB.get(any())).thenReturn("notanumber");
            MetricAccessor metricAccessorA = new MetricAccessor("my_metric1", dataA, Collections.emptyMap(), Collections.emptyMap());
            MetricAccessor metricAccessorB = new MetricAccessor("my_metric2", dataB, Collections.emptyMap(), Collections.emptyMap());

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(dataB).get(any());
            verify(measureMap, times(1)).record(eq(Tags.getTagger().empty()));
            verify(metricsManager, times(1)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));

            rec.execute(executionContext);

            verify(dataB, times(2)).get(any());
            verify(measureMap, times(2)).record(eq(Tags.getTagger().empty()));
            verify(metricsManager, times(2)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(2)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));
        }

        @Test
        void commonTagsIncluded() {
            when(inspectitContext.getData("common")).thenReturn("overwrite");
            when(commonTagsManager.getCommonTagKeys()).thenReturn(Collections.singletonList(TagKey.create("common")));

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().emptyBuilder().putLocal(TagKey.create("common"), TagValue.create("overwrite")).build();
            verify(measureMap, times(1)).record(eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
            verifyNoMoreInteractions(inspectitContext);
        }

        @Test
        void constantTags() {
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.singletonMap("constant", "tag"), Collections.emptyMap());
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().emptyBuilder().putLocal(TagKey.create("constant"), TagValue.create("tag")).build();
            verify(measureMap, times(1)).record(eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }

        @Test
        void dataTagsNotAvailable() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn(null);
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().empty();
            verify(measureMap, times(1)).record(eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }

        @Test
        void dataTags() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(mockAccessor.get(any())).thenReturn("value");

            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);
            when(variableAccess.get(any())).thenReturn(100L);

            MetricAccessor metricAccessor = new MetricAccessor("my_metric", variableAccess, Collections.emptyMap(), Collections.singletonMap("data", mockAccessor));
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().emptyBuilder().putLocal(TagKey.create("data"), TagValue.create("value")).build();
            verify(measureMap, times(1)).record(eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
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

            MetricsRecorder rec = new MetricsRecorder(Arrays.asList(metricAccessorA, metricAccessorB), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            InOrder inOrder = inOrder(measureMap, metricsManager);
            // first recording
            inOrder.verify(metricsManager).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));
            TagContext expected1 = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("cA"), TagValue.create("100"))
                    .putLocal(TagKey.create("existing"), TagValue.create("data1"))
                    .build();
            inOrder.verify(measureMap).record(eq(expected1));
            // second recording
            inOrder.verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric2"), same(measureMap), eq((Number) 200.0d));
            TagContext expected2 = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("cA"), TagValue.create("200"))
                    .putLocal(TagKey.create("existing1"), TagValue.create("12"))
                    .putLocal(TagKey.create("existing2"), TagValue.create("false"))
                    .build();
            inOrder.verify(measureMap).record(eq(expected2));
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
            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(metricAccessor), commonTagsManager, metricsManager, statsRecorder);

            rec.execute(executionContext);

            TagContext expected = Tags.getTagger().emptyBuilder().putLocal(TagKey.create("data"), TagValue.create("value")).build();
            verify(measureMap, times(1)).record(eq(expected));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }
    }
}
