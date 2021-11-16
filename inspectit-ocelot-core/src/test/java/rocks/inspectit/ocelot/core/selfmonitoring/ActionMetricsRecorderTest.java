package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link rocks.inspectit.ocelot.core.selfmonitoring.ActionsMetricsRecorder}
 */
@ExtendWith(MockitoExtension.class)
public class ActionMetricsRecorderTest extends SpringTestBase {

    @Mock
    private SelfMonitoringService selfMonitoringService;

    @Autowired
    @InjectMocks
    private ActionsMetricsRecorder recorder;

    @Nested
    class ActionMetrics extends SpringTestBase {

        @BeforeEach
        private void disableAllActionsMetrics() {
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.count", "false");
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.execution-time", "false");
            });
            assertThat(recorder.isEnabled()).isFalse();
        }

        @Test
        @DirtiesContext
        public void testExecutionIncrement() {
            assertThat(recorder.isEnabled()).isFalse();

            // make sure that the count/increment metrics is enabled.
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.count", "true");
            });
            assertThat(recorder.isEnabled()).isTrue();

            // record fake measurement
            recorder.record("my-action", 1000L);

            // verify that the increment has been recorded
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1L), anyMap());

            // verify that no other unverified interactions are left
            verifyNoMoreInteractions(selfMonitoringService);
        }

        @Test
        @DirtiesContext
        public void testExecutionTime() {
            assertThat(recorder.isEnabled()).isFalse();

            // make sure that the execution time metrics is enabled
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.execution-time", "true");
            });

            assertThat(recorder.isEnabled()).isTrue();

            // record fake measurement
            recorder.record("my-action", 1000L);

            // verify that the execution time has been recorded
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1000L), anyMap());

            // verify that no other unverified interactions are left
            verifyNoMoreInteractions(selfMonitoringService);
        }

        @Test
        @DirtiesContext
        public void testAllMetrics() {
            assertThat(recorder.isEnabled()).isFalse();
            // enable all metrics
            enableAllMetrics();
            // verify the recorder is enabled
            assertThat(recorder.isEnabled()).isTrue();

            // record fake measurement
            recorder.record("my-action", 1000L);

            // verify increment
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1L), anyMap());

            // verify execution time
            verify(selfMonitoringService, times(1)).recordMeasurement(anyString(), eq(1000L), anyMap());

            // verify that no other unverified interactions are left
            verifyNoMoreInteractions(selfMonitoringService);
        }

        /**
         * Enables all metrics for the {@link ActionsMetricsRecorder}
         */
        private void enableAllMetrics() {
            // enable all metrics.
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.execution-time", "true");
                mp.setProperty("inspectit.selfMonitoring.actions.enabled.count", "true");
            });
        }

    }

}
