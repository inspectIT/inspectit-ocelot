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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test for the building process of the {@link rocks.inspectit.ocelot.rest.agent.AgentService.SupportArchiveData support archive} in the {@link AgentService}
 */
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

        public void setupTestData(boolean logPreloadingEnabled) {
            serviceSpy = Mockito.spy(cut);
            String configYaml = String.format("inspectit.log-preloading: {enabled: %b}", logPreloadingEnabled);

            //set config
            config = AgentConfiguration.create(null, new HashSet<>(), configYaml);

            //set attributes
            attributes = new HashMap<String, String>() {{
                put("agent-id", "test");
            }};

            //set logs expected result
            logs = logPreloadingEnabled ? "logs: thisisalog" : AgentService.LOG_PRELOADING_DISABLED_MESSAGE;

            //set environment expected result
            environmentDetail = new EnvironmentCommand.EnvironmentDetail() {{
                setEnvironmentVariables(System.getenv());
                setSystemProperties(System.getProperties());
                setJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments());
            }};

            //create expected result object
            expectedResult = new AgentService.SupportArchiveData() {{
                setEnvironmentDetails(environmentDetail);
                setLogs(logs);
                setCurrentConfig(config.getConfigYaml());
            }};
        }

        public void setupTestObject(boolean logPreloadingEnabled) throws ExecutionException {
            DeferredResult<ResponseEntity<?>> envResult = new DeferredResult<ResponseEntity<?>>() {{
                setResult(ResponseEntity.ok().body(environmentDetail));
            }};

            when(configuration.getAgentCommand()).thenReturn(new AgentCommandSettings());
            when(configManager.getConfiguration(anyMap())).thenReturn(config);
            doReturn(envResult).when(serviceSpy).environment(anyString());

            if (logPreloadingEnabled) {
                DeferredResult<ResponseEntity<?>> logsResult = new DeferredResult<ResponseEntity<?>>() {{
                    setResult(ResponseEntity.ok().body(logs));
                }};

                doReturn(logsResult).when(serviceSpy).logs(anyString());
            }
        }

        @Test
        public void shouldBuildSupportArchive() throws ExecutionException {
            setupTestData(true);
            setupTestObject(true);

            DeferredResult<ResponseEntity<?>> actualResult = serviceSpy.buildSupportArchive(attributes, configManager);
            ResponseEntity<?> unwrappedResult = (ResponseEntity<?>) actualResult.getResult();

            assertThat(unwrappedResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(unwrappedResult.getBody()).isEqualTo(expectedResult);
        }

        @Test
        public void logPreloadingDisabled() throws ExecutionException {
            setupTestData(false);
            setupTestObject(false);

            DeferredResult<ResponseEntity<?>> actualResult = serviceSpy.buildSupportArchive(attributes, configManager); // Result of calling the method
            ResponseEntity<?> unwrappedResult = (ResponseEntity<?>) actualResult.getResult();
            AgentService.SupportArchiveData supportArchiveData = (AgentService.SupportArchiveData) unwrappedResult.getBody();

            assertThat(unwrappedResult.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(supportArchiveData).isEqualTo(expectedResult);
            assertThat(supportArchiveData.getLogs()).isEqualTo(AgentService.LOG_PRELOADING_DISABLED_MESSAGE);
            verify(serviceSpy, never()).logs(anyString());
        }
    }
}
