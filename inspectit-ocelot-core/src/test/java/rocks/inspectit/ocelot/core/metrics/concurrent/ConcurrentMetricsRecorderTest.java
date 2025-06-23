package rocks.inspectit.ocelot.core.metrics.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentMetricsRecorderTest extends SpringTestBase {

    @Autowired
    ConcurrentMetricsRecorder recorder;

    @BeforeEach
    void beforeEach() {
        updateProperties((mp) -> {
            mp.setProperty("inspectit.metrics.concurrent.enabled.invocations", "true");
        });
    }

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
                mp.setProperty("inspectit.metrics.concurrent.enabled.invocations", "false");
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
