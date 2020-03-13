package rocks.inspectit.ocelot.core.config.propertysources;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.IAgent;
import rocks.inspectit.ocelot.core.testutils.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentInformationPropertySourceTest {

    @Nested
    class Constructor {

        @Test
        public void verifyJavaVersion() {
            String version = System.getProperty("java.version");

            EnvironmentInformationPropertySource result = new EnvironmentInformationPropertySource("test");

            assertThat(result.getProperty("inspectit.env.java-version")).isEqualTo(version);
        }

        @Test
        public void verifyAgentVersion() {
            IAgent mockAgent = mock(IAgent.class);
            when(mockAgent.getVersion()).thenReturn("1.0");
            ReflectionUtils.writeStaticField(AgentManager.class, "agentInstance", mockAgent);

            EnvironmentInformationPropertySource result = new EnvironmentInformationPropertySource("test");

            assertThat(result.getProperty("inspectit.env.agent-version")).isEqualTo("1.0");
        }

        @Test
        public void verifyAgentVersionWithNullAgent() {
            ReflectionUtils.writeStaticField(AgentManager.class, "agentInstance", null);

            EnvironmentInformationPropertySource result = new EnvironmentInformationPropertySource("test");

            assertThat(result.getProperty("inspectit.env.agent-version")).isEqualTo("UNKNOWN");
        }
    }
}