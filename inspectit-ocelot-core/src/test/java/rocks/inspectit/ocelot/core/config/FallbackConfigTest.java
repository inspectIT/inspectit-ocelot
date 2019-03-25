package rocks.inspectit.ocelot.core.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

@TestPropertySource(properties = {
        "inspectit.thread-pool-size=iAmNotANumber"
})
public class FallbackConfigTest extends SpringTestBase {

    @Autowired
    InspectitEnvironment env;

    @Test
    public void testFallbackConfigurationActive() {
        Assertions.assertThat(env.getCurrentConfig().getThreadPoolSize()).isEqualTo(2);
        Assertions.assertThat(env.getCurrentConfig().getMetrics().isEnabled()).isFalse();
    }
}
