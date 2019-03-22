package rocks.inspectit.ocelot.core.config;

import ch.qos.logback.classic.Level;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

public class InvalidConfigurationUpdateTest extends SpringTestBase {

    @Autowired
    InspectitEnvironment env;

    @Test
    @DirtiesContext
    public void testPreviousConfigMaintainedOnInvalidUpdate() {
        updateProperties(mp -> {
            mp.setProperty("inspectit.service-name", "ConfA");
        });
        Assertions.assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("ConfA");
        assertNoLogsOfLevelOrGreater(Level.WARN);
        updateProperties(mp -> {
            mp.setProperty("inspectit.service-name", "ConfB");
            mp.setProperty("inspectit.thread-pool-size", "-1");
        });
        assertLogsOfLevelOrGreater(Level.ERROR);
        Assertions.assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("ConfA");

        //Ensure that fixing the config is possible
        updateProperties(mp -> {
            mp.setProperty("inspectit.service-name", "ConfC");
            mp.setProperty("inspectit.thread-pool-size", "2");
        });
        Assertions.assertThat(env.getCurrentConfig().getServiceName()).isEqualTo("ConfC");
        Assertions.assertThat(env.getCurrentConfig().getThreadPoolSize()).isEqualTo(2);
    }
}
