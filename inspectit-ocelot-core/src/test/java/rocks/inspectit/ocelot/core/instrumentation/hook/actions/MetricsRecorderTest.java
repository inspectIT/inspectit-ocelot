package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
            VariableAccessor variableAccess = Mockito.mock(VariableAccessor.class);

            when(variableAccess.get(any())).thenReturn(null);

            MetricsRecorder rec = new MetricsRecorder(Collections.singletonList(Pair.of("my_metric", variableAccess)), metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(measureMap, times(1)).record();
            verify(metricsManager, never()).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));

            when(variableAccess.get(any())).thenReturn(100L);

            rec.execute(executionContext);

            verify(measureMap, times(2)).record();
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric"), same(measureMap), eq((Number) 100L));
        }


        @Test
        void verifyInvalidDataTypeHandled() {

            VariableAccessor dataA = Mockito.mock(VariableAccessor.class);
            VariableAccessor dataB = Mockito.mock(VariableAccessor.class);
            when(dataA.get(any())).thenReturn(100.0);
            when(dataB.get(any())).thenReturn("notanumber");

            List<Pair<String, VariableAccessor>> metricsToData = Arrays.asList(
                    Pair.of("my_metric1", dataA),
                    Pair.of("my_metric2", dataB)
            );

            MetricsRecorder rec = new MetricsRecorder(metricsToData, metricsManager, statsRecorder);

            rec.execute(executionContext);

            verify(dataB).get(any());
            verify(measureMap, times(1)).record();
            verify(metricsManager, times(1)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(1)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));

            rec.execute(executionContext);

            verify(dataB, times(2)).get(any());
            verify(measureMap, times(2)).record();
            verify(metricsManager, times(2)).tryRecordingMeasurement(any(String.class), same(measureMap), any(Number.class));
            verify(metricsManager, times(2)).tryRecordingMeasurement(eq("my_metric1"), same(measureMap), eq((Number) 100.0d));
        }
    }
}
