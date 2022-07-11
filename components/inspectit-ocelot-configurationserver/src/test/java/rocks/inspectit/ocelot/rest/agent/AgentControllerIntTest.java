package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.config.model.AgentCommandSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AgentControllerIntTest extends IntegrationTestBase {

    @Autowired
    InspectitServerSettings serverSettings;

    @BeforeEach
    private void resetServerSettings() {
        serverSettings.setAgentCommand(AgentCommandSettings.builder().build());
    }

    private ResponseEntity<Command> fetchCommand(String agentId, CommandResponse response, boolean wait) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-ocelot-agent-id", agentId);
        HttpEntity<CommandResponse> request = new HttpEntity<>(response, httpHeaders);

        String url = "/api/v1/agent/command";
        if (wait) {
            url += "?wait-for-command=true";
        }

        return rest.postForEntity(url, request, Command.class);
    }

    @Test
    public void shouldFetchSupportArchive() throws InterruptedException {

        String agentId = "testAgent";
        String testLog = "greatLog";
        EnvironmentCommand.EnvironmentDetail envDetail = new EnvironmentCommand.EnvironmentDetail() {{
            setEnvironmentVariables(Collections.emptyMap());
            setSystemProperties(System.getProperties());
            setJvmArguments(Collections.emptyList());
        }};
        AgentService.SupportArchiveData expectedResult = new AgentService.SupportArchiveData() {{
            setCurrentConfig("");
            setEnvironmentDetails(envDetail);
            setLogs(testLog);
        }};

        new Thread(() -> {
            ResponseEntity<Command> resultFirstCommand = fetchCommand(agentId, null, true);

            EnvironmentCommand envCommand = (EnvironmentCommand) resultFirstCommand.getBody();
            assertThat(envCommand).isNotNull();

            EnvironmentCommand.Response envResponse = new EnvironmentCommand.Response(envDetail);
            envResponse.setCommandId(envCommand.getCommandId());

            ResponseEntity<Command> resultSecondCommand = fetchCommand(agentId, envResponse, true);

            LogsCommand logsCommand = (LogsCommand) resultSecondCommand.getBody();
            assertThat(logsCommand).isNotNull();

            LogsCommand.Response logsResponse = new LogsCommand.Response(testLog);
            logsResponse.setCommandId(logsCommand.getCommandId());
            fetchCommand(agentId, logsResponse, false);
        }).start();

        AtomicReference<ResponseEntity<?>> archiveResult = new AtomicReference<>();
        new Thread(() -> archiveResult.set(authRest.getForEntity("/api/v1/agent/supportArchive?agent-id=" + agentId, AgentService.SupportArchiveData.class))).start();

        await().atMost(3, TimeUnit.SECONDS).until(archiveResult::get, Objects::nonNull);
        assertThat(archiveResult.get().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(archiveResult.get().getBody()).isEqualTo(expectedResult);
    }

    @Test
    public void shouldTimeoutWhileFetchingSupportArchive() {
        serverSettings.getAgentCommand().setResponseTimeout(Duration.ofSeconds(1));
        serverSettings.getAgentCommand().setCommandTimeout(Duration.ofSeconds(1));

        String agentId = "timeoutTestAgent";

        AtomicReference<ResponseEntity<?>> archiveResult = new AtomicReference<>();
        new Thread(() -> archiveResult.set(authRest.getForEntity("/api/v1/agent/supportArchive?agent-id=" + agentId, AgentService.SupportArchiveData.class))).start();

        await().atMost(5, TimeUnit.SECONDS).until(archiveResult::get, Objects::nonNull);
        assertThat(archiveResult.get().getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
        assertThat(archiveResult.get().getBody()).isNull();
    }

    @Test
    public void fetchCommand() throws InterruptedException {
        ResponseEntity<Command> result = fetchCommand("drogon ", null, false);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        new Thread(() -> {
            authRest.getForEntity("/api/v1/command/ping?agent-id=drogon", Void.class);
        }).start();

        Thread.sleep(1000);

        ResponseEntity<Command> resultSecond = fetchCommand("drogon", null, false);
        assertThat(resultSecond.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultSecond.getBody()).isInstanceOf(PingCommand.class);

        ResponseEntity<Command> resultThird = fetchCommand("drogon ", null, false);
        assertThat(resultThird.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void waitForCommand() throws InterruptedException {
        // agent is fetching a command and waits for it
        new Thread(() -> {
            // fetch and wait for a command
            ResponseEntity<Command> resultEntity = fetchCommand("drogon ", null, true);

            PingCommand command = (PingCommand) resultEntity.getBody();
            assertThat(command).isNotNull();

            // send the command response
            PingCommand.Response response = new PingCommand.Response(command.getCommandId());
            fetchCommand("drogon ", response, false);
        }).start();

        Thread.sleep(1000);

        // pinging the agent
        AtomicReference<ResponseEntity<Void>> getResult = new AtomicReference<>();
        new Thread(() -> {
            getResult.set(authRest.getForEntity("/api/v1/command/ping?agent-id=drogon", Void.class));
        }).start();

        await().atMost(2, TimeUnit.SECONDS).until(getResult::get, Objects::nonNull);

        assertThat(getResult.get().getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
