package rocks.inspectit.oce.core.instrumentation.hook;

import io.opencensus.stats.Measure;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.metrics.MeasuresAndViewsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricsRecorderTest {

    @Mock
    MeasuresAndViewsManager metricsManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StatsRecorder statsRecorder;

    @Mock
    MeasureMap measureMap;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IHookAction.ExecutionContext executionContext;

    @BeforeEach
    void setupMock() {
        when(statsRecorder.newMeasureMap()).thenReturn(measureMap);
    }

    @Nested
    class Execute {

        @Test
        void verifyMissingConstantMetricIgnored() {
            when(metricsManager.getMeasure(eq("my_metric"))).thenReturn(Optional.empty());
            MetricsRecorder rec = new MetricsRecorder(Maps.newHashMap("my_metric", 42.0), Collections.emptyMap(), metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(measureMap, times(1)).record();
            verify(measureMap, never()).put(any(Measure.MeasureLong.class), any(Long.class));
            verify(measureMap, never()).put(any(Measure.MeasureDouble.class), any(Double.class));

            Measure.MeasureLong measure = Mockito.mock(Measure.MeasureLong.class);
            when(metricsManager.getMeasure(eq("my_metric"))).thenReturn(Optional.of(measure));


            rec.execute(executionContext);

            verify(measureMap, times(2)).record();
            verify(measureMap, times(1)).put(any(Measure.MeasureLong.class), eq(42L));
            verify(measureMap, never()).put(any(Measure.MeasureDouble.class), any(Double.class));
        }


        @Test
        void verifyNullValueDataMetricIgnored() {

            when(metricsManager.getMeasure(eq("my_metric"))).thenReturn(Optional.empty());
            when(executionContext.getInspectitContext().getData(eq("my_data"))).thenReturn(100);

            MetricsRecorder rec = new MetricsRecorder(Collections.emptyMap(), Maps.newHashMap("my_metric", "my_data"), metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(measureMap, times(1)).record();
            verify(measureMap, never()).put(any(Measure.MeasureLong.class), any(Long.class));
            verify(measureMap, never()).put(any(Measure.MeasureDouble.class), any(Double.class));

            Measure.MeasureDouble measure = Mockito.mock(Measure.MeasureDouble.class);
            when(metricsManager.getMeasure(eq("my_metric"))).thenReturn(Optional.of(measure));


            rec.execute(executionContext);

            verify(measureMap, times(2)).record();
            verify(measureMap, times(1)).put(any(Measure.MeasureDouble.class), eq(100.0));
            verify(measureMap, never()).put(any(Measure.MeasureLong.class), any(Long.class));
        }


        @Test
        void verifyInvalidDataTypeHandled() {

            Measure.MeasureDouble measure1 = Mockito.mock(Measure.MeasureDouble.class);
            Measure.MeasureDouble measure2 = Mockito.mock(Measure.MeasureDouble.class);
            doReturn(Optional.of(measure1)).when(metricsManager).getMeasure(eq("my_metric1"));
            lenient().doReturn(Optional.of(measure2)).when(metricsManager).getMeasure(eq("my_metric2"));
            when(executionContext.getInspectitContext().getData(any())).thenAnswer(invoc -> {
                String dataKey = invoc.getArgument(0);
                if ("my_data1".equals(dataKey)) {
                    return 100;
                } else if ("my_data2".equals(dataKey)) {
                    return "notanumber";
                } else {
                    return null;
                }
            });

            HashMap<String, String> metricsToData = new HashMap<>();
            metricsToData.put("my_metric1", "my_data1");
            metricsToData.put("my_metric2", "my_data2");

            MetricsRecorder rec = new MetricsRecorder(Collections.emptyMap(), metricsToData, metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(executionContext.getInspectitContext(), times(1)).getData(eq("my_data2"));
            verify(measureMap, times(1)).record();
            verify(measureMap, times(1)).put(any(Measure.MeasureDouble.class), eq(100.0));
            verify(measureMap, never()).put(any(Measure.MeasureLong.class), any(Long.class));

            rec.execute(executionContext);

            verify(executionContext.getInspectitContext(), times(1)).getData(eq("my_data2"));
            verify(measureMap, times(2)).record();
            verify(measureMap, times(2)).put(any(Measure.MeasureDouble.class), eq(100.0));
            verify(measureMap, never()).put(any(Measure.MeasureLong.class), any(Long.class));
        }
    }
}
