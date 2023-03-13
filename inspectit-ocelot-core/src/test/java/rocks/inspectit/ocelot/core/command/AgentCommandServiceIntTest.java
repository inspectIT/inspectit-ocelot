package rocks.inspectit.ocelot.core.command;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
public class AgentCommandServiceIntTest extends SpringTestBase {

    @Autowired
    AgentCommandService agentCommandService;

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(AgentCommandService.class, org.slf4j.event.Level.WARN);

    @Nested
    @DirtiesContext
    public class DeprecatedProperties {

        @Test
        public void deriveFromHttpConfig() {
            updateProperties(mps -> {
                mps.setProperty("inspectit.config.http.url", "http://localhost:8090/api/v1/agent/configuration");
                mps.setProperty("inspectit.agent-commands.enabled", Boolean.TRUE);
                mps.setProperty("inspectit.agent-commands.derive-from-http-config-url", true);
            });

            assertThat(agentCommandService.isEnabled()).isTrue();
            warnLogs.assertContains("derive-from-http-config-url");
        }
    }
}
