package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.LogsCommand;
import rocks.inspectit.ocelot.grpc.LogsCommandResponse;

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
        return command.hasLogs();
    }

    /**
     * Checks if the given {@link CommandResponse} is an instance of {@link LogsCommandResponse}.
     *
     * @param response The response which should be checked.
     *
     * @return True if the given response is an instance of {@link LogsCommandResponse}.
     */
    @Override
    public boolean canHandle(CommandResponse response) {
        return response.hasLogs();
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
        LogsCommandResponse logsResponse = response.getLogs();
        result.setResult(ResponseEntity.ok().body(logsResponse.getLogs()));
    }

}
