package rocks.inspectit.ocelot.core.selfmonitoring;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the configuration for {@link ActionMetricsRecorder}
 */
public class ActionMetricsRecorderConfigTest extends SpringTestBase {

    @Autowired
    private ActionMetricsRecorder recorder;

    @Nested
    class Defaults extends SpringTestBase {

        @Test
        void checkDefaultEnabled() {
            assertThat(recorder.isEnabled()).isTrue();
        }
    }

    @Nested
    class CheckConfigChanges extends SpringTestBase {

        @Test
        @DirtiesContext
        void checkAllEnabled() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.action-metrics.enabled", "true");
            });
            assertThat(recorder.isEnabled()).isTrue();
        }

        @Test
        @DirtiesContext
        void checkAllDisabled() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.selfMonitoring.action-metrics.enabled", "false");
            });
            assertThat(recorder.isEnabled()).isFalse();
        }
    }
}
