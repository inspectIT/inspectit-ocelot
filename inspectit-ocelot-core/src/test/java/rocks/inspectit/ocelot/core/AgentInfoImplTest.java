package rocks.inspectit.ocelot.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AgentInfoImplTest {

    private AgentInfoImpl agentInfo;

    @Test
    void shouldReturnCurrentVersion() {
        agentInfo = new AgentInfoImpl("2.6.12");

        assertThat(agentInfo.currentVersion()).isEqualTo("2.6.12");
    }

    @Test
    void shouldReturnTrueForSnapshotVersion() {
        agentInfo = new AgentInfoImpl("SNAPSHOT");

        assertThat(agentInfo.isAtLeastVersion("9.9.9")).isTrue();
        assertThat(agentInfo.isAtLeastVersion("0.0.1")).isTrue();
    }

    @Test
    void shouldReturnFalseForUnknownVersion() {
        agentInfo = new AgentInfoImpl("UNKNOWN");

        assertThat(agentInfo.isAtLeastVersion("0.0.0")).isFalse();
        assertThat(agentInfo.isAtLeastVersion("9.9.9")).isFalse();
    }

    @Test
    void shouldReturnFalseForLowerVersion() {
        agentInfo = new AgentInfoImpl("1.2.3");

        assertThat(agentInfo.isAtLeastVersion("1.2.4")).isFalse();
        assertThat(agentInfo.isAtLeastVersion("1.3.0")).isFalse();
        assertThat(agentInfo.isAtLeastVersion("2.0.0")).isFalse();
    }

    @Test
    void shouldReturnTrueForHigherVersion() {
        agentInfo = new AgentInfoImpl("2.6.12");

        assertThat(agentInfo.isAtLeastVersion("2.6.10")).isTrue();
        assertThat(agentInfo.isAtLeastVersion("2.5.9")).isTrue();
        assertThat(agentInfo.isAtLeastVersion("2.4.99")).isTrue();
        assertThat(agentInfo.isAtLeastVersion("1.99.99")).isTrue();
    }

    @Test
    void testIsAtLeastVersion_withInvalidVersionFormatThrows() {
        agentInfo = new AgentInfoImpl("1.2.3.4");
        assertThatThrownBy(() -> agentInfo.isAtLeastVersion("1.2.3"))
                .isInstanceOf(IllegalArgumentException.class);

        agentInfo = new AgentInfoImpl("1.2.3");
        assertThatThrownBy(() -> agentInfo.isAtLeastVersion("1.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
