package rocks.inspectit.oce.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.oce.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "inspectit.thread-pool-size=iAmNotANumber"
})
public class FallbackConfigTest extends SpringTestBase {

    @Autowired
    InspectitEnvironment env;

    @Test
    public void testFallbackConfigurationActive() {
        assertThat(env.getCurrentConfig().getThreadPoolSize()).isEqualTo(2);
        assertThat(env.getCurrentConfig().getMetrics().isEnabled()).isFalse();
    }
}
