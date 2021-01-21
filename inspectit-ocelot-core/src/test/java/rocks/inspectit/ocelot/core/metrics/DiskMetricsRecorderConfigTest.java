package rocks.inspectit.ocelot.core.metrics;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.metrics.system.DiskMetricsRecorder;

import static org.assertj.core.api.Assertions.assertThat;

public class DiskMetricsRecorderConfigTest extends SpringTestBase {

    @Autowired
    DiskMetricsRecorder recorder;

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
        void checkTotalDisabled() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.metrics.disk.enabled.total", "false");
            });
            assertThat(recorder.isEnabled()).isTrue();
        }

        @Test
        @DirtiesContext
        void checkAllDisabled() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.metrics.disk.enabled.total", "false");
                mp.setProperty("inspectit.metrics.disk.enabled.free", "false");
            });
            assertThat(recorder.isEnabled()).isFalse();
        }

        @Test
        @DirtiesContext
        void checkMasterSwitch() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.metrics.enabled", "false");
            });
            assertThat(recorder.isEnabled()).isFalse();
        }


        @Test
        @DirtiesContext
        void checkRestart() {
            assertThat(recorder.isEnabled()).isTrue();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.metrics.enabled", "false");
            });
            assertThat(recorder.isEnabled()).isFalse();
            updateProperties((mp) -> {
                mp.setProperty("inspectit.metrics.enabled", "true");
            });
            assertThat(recorder.isEnabled()).isTrue();
        }
    }
}
