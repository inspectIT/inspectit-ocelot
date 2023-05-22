package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.utils.RetryUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Component which handles the fetching of new agent commands and execution of it.
 */
@Slf4j
@Component
public class CommandHandler {

    @Autowired
    protected InspectitEnvironment environment;

    @Autowired
    private HttpCommandFetcher commandFetcher;

    /**
     * Used to delegate recieved {@link Command} objects to their respective implementation of {@link rocks.inspectit.ocelot.core.command.handler.CommandExecutor}.
     */
    @Autowired
    private CommandDelegator commandDelegator;

    /**
     * Object mapper for deserializing commands.
     */
    @VisibleForTesting
    ObjectMapper objectMapper = new ObjectMapper().enableDefaultTyping();

    /**
     * Timestamp when the agent switched into live mode.
     */
    private long liveModeStart = 0L;

    /**
     * Whether the agent is in live mode.
     */
    private boolean liveMode = false;

    /**
     * Tries fetching and executing a new agent command from the server.
     */
    public void nextCommand() {
        nextCommand(null);
    }

    /**
     * Tries fetching and executing a new agent command from the server. The given command response will be sent with the
     * request as a piggyback payload. In case the server returns any command, it will be executed and the handler may
     * switch to live mode where it executes more requests in order to get further agent commands.
     *
     * @param payload a {@link CommandResponse} to send with the next request
     */
    private void nextCommand(CommandResponse payload) {
        CommandResponse commandResponse = payload;

        do {
            // fetch and execute next command and return optional payload
            Command command = getCommandWithRetry(commandResponse);
            commandResponse = executeCommand(command);

            if (commandResponse != null) {
                // start / continue live mode
                if (liveMode) {
                    log.debug("Extending command live mode timeout.");
                } else {
                    log.debug("Switching to command live mode.");
                    liveMode = true;
                }
                liveModeStart = System.currentTimeMillis();
            }

            if (liveMode && isLiveModeExpired()) {
                log.debug("Leaving command live mode.");
                liveMode = false;
            }
        } while (liveMode || commandResponse != null);
    }

    /**
     * @return Returns true if the live mode has been expired.
     */
    private boolean isLiveModeExpired() {
        AgentCommandSettings settings = environment.getCurrentConfig().getAgentCommands();
        return System.currentTimeMillis() >= liveModeStart + settings.getLiveModeDuration().toMillis();
    }

    private Command getCommandWithRetry(CommandResponse commandResponse) {
        Retry retry = buildRetry();
        if (retry != null) {
            return retry.executeSupplier(() -> getCommand(commandResponse));
        } else {
            return getCommand(commandResponse);
        }
    }

    private Retry buildRetry() {
        return RetryUtils.buildRetry(environment.getCurrentConfig()
                .getAgentCommands()
                .getRetry(), "agent-commands");
    }

    /**
     * Fetches a command and processes the response.
     *
     * @param commandResponse A CommandResponse
     *
     * @return The command
     *
     * @throws IllegalStateException If communication was not successful.
     */
    private Command getCommand(CommandResponse commandResponse) {
        HttpResponse response;
        try {
            response = commandFetcher.fetchCommand(commandResponse, liveMode);
        } catch (IOException ioe) {
            throw new IllegalStateException("IOException while fetching command", ioe);
        }
        // response is null if some configurations are not correct. commandFetcher will log an error in this case.
        if (response == null) {
            return null;
        }

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_NO_CONTENT) {
            // we do nothing
            return null;
        } else if (statusCode == HttpStatus.SC_OK) {
            try {
                HttpEntity responseEntity = response.getEntity();
                InputStream content = responseEntity.getContent();
                return objectMapper.readValue(content, Command.class);
            } catch (Exception exception) {
                log.error("Exception during agent command deserialization.", exception);
                return null;
            }
        } else {
            throw new IllegalStateException("Couldn't successfully fetch an agent command. Server returned " + response.getStatusLine()
                    .getStatusCode());
        }
    }

    /**
     * Executes the given command in case it is not null.
     *
     * @param command the command to execute
     *
     * @return the commands result as a {@link CommandResponse}
     */
    private CommandResponse executeCommand(Command command) {
        if (command != null) {
            try {
                return commandDelegator.delegate(command);
            } catch (Exception exception) {
                log.error("Exception during agent command execution.", exception);
            }
        }
        return null;
    }
}
