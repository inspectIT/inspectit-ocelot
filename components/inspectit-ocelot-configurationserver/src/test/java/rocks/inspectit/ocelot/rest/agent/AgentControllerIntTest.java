package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AgentControllerIntTest extends IntegrationTestBase {

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
