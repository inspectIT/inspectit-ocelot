package rocks.inspectit.ocelot.agentstatus;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentMetaInformationTest {

    @Nested
    class Of {

        @Test
        public void nullMap() {
            AgentMetaInformation result = AgentMetaInformation.of(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptyMap() {
            AgentMetaInformation result = AgentMetaInformation.of(Collections.emptyMap());

            assertThat(result).isNull();
        }

        @Test
        public void noMatchingHeaders() {
            Map<String, String> map = Collections.singletonMap("any", "value");

            AgentMetaInformation result = AgentMetaInformation.of(map);

            assertThat(result).isNull();
        }

        @Test
        public void matchingHeaders() {
            Map<String, String> map = Collections.singletonMap("x-ocelot-agent-id", "id-123");

            AgentMetaInformation result = AgentMetaInformation.of(map);

            assertThat(result).isNotNull();
            assertThat(result.getAgentId()).isEqualTo("id-123");
        }
    }

}