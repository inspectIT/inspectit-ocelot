package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.Collections;
import java.util.HashMap;

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
        void verifyNullValueDataMetricIgnored() {

            when(executionContext.getInspectitContext().getData(eq("my_data"))).thenReturn(null);

            MetricsRecorder rec = new MetricsRecorder(Collections.emptyMap(), Maps.newHashMap("my_metric", "my_data"), metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(measureMap, times(1)).record();
            verify(metricsManager, never()).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));

            when(executionContext.getInspectitContext().getData(eq("my_data"))).thenReturn(100L);

            rec.execute(executionContext);

            verify(measureMap, times(2)).record();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }


        @Test
        void verifyInvalidDataTypeHandled() {

            when(executionContext.getInspectitContext().getData(any())).thenAnswer(invoc -> {
                String dataKey = invoc.getArgument(0);
                if ("my_data1".equals(dataKey)) {
                    return 100.0;
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
            verify(metricsManager, times(1)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));

            rec.execute(executionContext);

            verify(executionContext.getInspectitContext(), times(1)).getData(eq("my_data2"));
            verify(measureMap, times(2)).record();
            verify(metricsManager, times(2)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(2)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));
        }
    }
}
