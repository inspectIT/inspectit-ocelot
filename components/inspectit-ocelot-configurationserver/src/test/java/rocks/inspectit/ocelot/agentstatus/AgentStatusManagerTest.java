package rocks.inspectit.ocelot.agentstatus;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AgentStatusManagerTest {

    public static final String HEADER_AGENT_ID = "x-ocelot-agent-id";

    @InjectMocks
    AgentStatusManager manager;

    @BeforeEach
    void init() {
        manager.config = InspectitServerSettings.builder()
                .maxAgents(100)
                .agentEvictionDelay(Duration.ofDays(1))
                .build();
        manager.reset();
    }

    @Nested
    class NotifyAgentConfigurationFetched {

        @Test
        void testWithAgentIdHeader() {
            Branch testBranch = Branch.WORKSPACE;
            AgentMapping agentMapping = AgentMapping.builder().name("test-conf").sourceBranch(testBranch).build();
            AgentConfiguration config = AgentConfiguration.builder().mapping(agentMapping).configYaml("").build();
            Map<String, String> attributes = ImmutableMap.of("service", "test");

            manager.notifyAgentConfigurationFetched(attributes, Collections.singletonMap(HEADER_AGENT_ID, "aid"), config);

            assertThat(manager.getAgentStatuses()).hasSize(1).anySatisfy(status -> {
                assertThat(status.getAttributes()).isEqualTo(attributes);
                assertThat(status.getMappingName()).isEqualTo("test-conf");
                assertThat(status.getMetaInformation().getAgentId()).isEqualTo("aid");
                assertThat(status.getSourceBranch()).isEqualTo("workspace");
                assertThat(status.getLastConfigFetch()).isNotNull();
            });
        }

        @Test
        void testNoMappingFound() {
            Map<String, String> attributes = ImmutableMap.of("service", "test");
            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), null);

            assertThat(manager.getAgentStatuses()).hasSize(1).anySatisfy(status -> {
                assertThat(status.getAttributes()).isEqualTo(attributes);
                assertThat(status.getMappingName()).isNull();
                assertThat(status.getMetaInformation()).isNull();
                assertThat(status.getSourceBranch()).isNull();
                assertThat(status.getLastConfigFetch()).isNotNull();
            });
        }

        @Test
        void testMappingFound() {
            Branch testBranch = Branch.WORKSPACE;
            AgentMapping agentMapping = AgentMapping.builder().name("test-conf").sourceBranch(testBranch).build();
            AgentConfiguration conf = AgentConfiguration.builder().mapping(agentMapping).configYaml("").build();
            Map<String, String> attributes = ImmutableMap.of("service", "test");

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), conf);

            assertThat(manager.getAgentStatuses()).hasSize(1).anySatisfy(status -> {
                assertThat(status.getAttributes()).isEqualTo(attributes);
                assertThat(status.getMappingName()).isEqualTo("test-conf");
                assertThat(status.getSourceBranch()).isEqualTo("workspace");
                assertThat(status.getLastConfigFetch()).isNotNull();
            });
        }

        @Test
        void testOverriding() throws Exception {
            Branch testBranch = Branch.WORKSPACE;
            AgentMapping agentMapping = AgentMapping.builder().name("test-conf").sourceBranch(testBranch).build();
            AgentConfiguration conf = AgentConfiguration.builder().mapping(agentMapping).configYaml("").build();
            Map<String, String> attributes = ImmutableMap.of("service", "test");

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), null);

            assertThat(manager.getAgentStatuses()).hasSize(1).anySatisfy(status -> {
                assertThat(status.getAttributes()).isEqualTo(attributes);
                assertThat(status.getMappingName()).isNull();
                assertThat(status.getLastConfigFetch()).isNotNull();
            });

            Date firstFetch = manager.getAgentStatuses().iterator().next().getLastConfigFetch();

            Thread.sleep(1);

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), conf);

            assertThat(manager.getAgentStatuses()).hasSize(1).anySatisfy(status -> {
                assertThat(status.getAttributes()).isEqualTo(attributes);
                assertThat(status.getMappingName()).isEqualTo("test-conf");
                assertThat(status.getSourceBranch()).isEqualTo("workspace");
                assertThat(status.getLastConfigFetch()).isAfter(firstFetch);
            });
        }
    }
}
