package rocks.inspectit.ocelot.core.metrics.timewindow.worker;

import io.opentelemetry.api.baggage.Baggage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.metrics.timewindow.TimeWindowViewManager;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.TimeWindowView;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeWindowRecorderTest {

    @InjectMocks
    TimeWindowRecorder recorder;

    @Mock
    TimeWindowViewManager viewManager;

    @Mock
    TimeWindowView view;

    String metricName = "my_metric";

    @Test
    void shouldRecordValueForView() {
        when(viewManager.areAnyViewsRegistered(metricName)).thenReturn(true);
        when(viewManager.getViews(metricName)).thenReturn(Collections.singletonList(view));
        Baggage baggage = Baggage.current();

        recorder.recordMetric(metricName, 42.0, baggage);
        recorder.record();

        verify(view).insertValue(eq(42.0), any(Instant.class), eq(baggage));
    }
}
