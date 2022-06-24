package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfigurationManager;
import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.config.model.AgentCommandSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentServiceTest {

    @InjectMocks
    AgentService cut;
    @Mock
    AgentConfigurationManager configManager;
    @Mock
    InspectitServerSettings configuration;

    @Nested
    public class BuildSupportArchive {
        AgentService serviceSpy;
        AgentConfiguration config;
        Map<String, String> attributes;
        String logs;
        EnvironmentCommand.EnvironmentDetail environmentDetail;
        AgentService.SupportArchiveData expectedResult;

        public void setupTestData() {
            serviceSpy = Mockito.spy(cut);
            config = AgentConfiguration.builder().configYaml("foo : bar").build();
            attributes = new HashMap<String, String>() {{
                put("agent-id", "test");
            }};

            logs = "logs: thisisalog";
            environmentDetail = new EnvironmentCommand.EnvironmentDetail() {{
                setEnvironmentVariables(System.getenv());
                setSystemProperties(System.getProperties());
                setJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments());
            }};
            expectedResult = new AgentService.SupportArchiveData() {{
                setEnvironmentDetails(environmentDetail);
                setLogs(logs);
                setCurrentConfig(config.getConfigYaml());
            }};
        }

        @Test
        public void shouldBuildSupportArchive() throws ExecutionException {
            setupTestData();

            DeferredResult<ResponseEntity<?>> envResult = new DeferredResult<ResponseEntity<?>>() {{
                setResult(ResponseEntity.ok().body(environmentDetail));
            }};
            DeferredResult<ResponseEntity<?>> logsResult = new DeferredResult<ResponseEntity<?>>() {{
                setResult(ResponseEntity.ok().body(logs));
            }};

            when(configuration.getAgentCommand()).thenReturn(new AgentCommandSettings());
            when(configManager.getConfiguration(anyMap())).thenReturn(config);
            doReturn(envResult).when(serviceSpy).environment(anyString());
            doReturn(logsResult).when(serviceSpy).logs(anyString());

            DeferredResult<ResponseEntity<?>> actualResult = serviceSpy.buildSupportArchive(attributes, configManager);
            ResponseEntity<?> unwrappedResult = (ResponseEntity<?>) actualResult.getResult();
            assertThat(unwrappedResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(unwrappedResult.getBody()).isEqualTo(expectedResult);
        }
    }
}
