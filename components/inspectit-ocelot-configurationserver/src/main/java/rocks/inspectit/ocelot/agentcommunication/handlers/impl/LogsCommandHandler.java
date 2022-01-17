package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;

/**
 * Handler for the Agent Logs check command.
 */
@Slf4j
@Component
public class LogsCommandHandler implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    /**
     * Checks if the given {@link Command} is an instance of {@link LogsCommand}.
     *
     * @param command The command which should be checked.
     *
     * @return True if the given command is an instance of {@link LogsCommand}.
     */
    @Override
    public boolean canHandle(Command command) {
        return command instanceof LogsCommand;
    }

    /**
     * Checks if the given {@link CommandResponse} is an instance of {@link LogsCommand.Response}.
     *
     * @param response The response which should be checked.
     *
     * @return True if the given response is an instance of {@link LogsCommand.Response}.
     */
    @Override
    public boolean canHandle(CommandResponse response) {
        return response instanceof LogsCommand.Response;
    }

    /**
     * Prepares an instance of {@link DeferredResult} by setting the Timeout as well as the onTimeout function.
     * This onTimeout function sets the {@link ResponseEntity} to the status REQUEST_TIMEOUT.
     *
     * @param agentId The id of the agent the command is meant for.
     * @param command The command to be Executed.
     *
     * @return An instance of  {@link DeferredResult} with a set timeout value and a set timeout function.
     */
    @Override
    public DeferredResult<ResponseEntity<?>> prepareResponse(String agentId, Command command) {
        if (!canHandle(command)) {
            throw new IllegalArgumentException("LogsCommandHandler can only handle commands of type LogsCommand.");
        }
        Duration responseTimeout = configuration.getAgentCommand().getResponseTimeout();
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(responseTimeout.toMillis());

        deferredResult.onTimeout(() -> ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build());

        return deferredResult;
    }

    /**
     * Takes an instance of {@link CommandResponse} as well as an instance of {@link DeferredResult}. Sets the
     * {@link ResponseEntity} of the {@link DeferredResult} to the status OK. In this handler the given response is
     * ignored since the response itself indicates that the agent is alive.
     *
     * @param response The {@link CommandResponse} to be handled.
     * @param result   The {@link DeferredResult} the response should be written in.
     */
    @Override
    public void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result) {
        LogsCommand.Response logsResponse = (LogsCommand.Response) response;
        result.setResult(ResponseEntity.ok().body(logsResponse.getResult()));
    }
}
