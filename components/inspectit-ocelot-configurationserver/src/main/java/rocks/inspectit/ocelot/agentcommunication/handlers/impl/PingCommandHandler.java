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
import rocks.inspectit.ocelot.grpc.PingCommand;
import rocks.inspectit.ocelot.grpc.PingCommandResponse;

/**
 * Handler for the Agent Health check command.
 */
@Slf4j
@Component
public class PingCommandHandler implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    /**
     * Checks if the given {@link Command} is an instance of {@link PingCommand}.
     *
     * @param command The command which should be checked.
     *
     * @return True if the given command is an instance of {@link PingCommand}.
     */
    @Override
    public boolean canHandle(Command command) {
        return command.hasPing();
    }

    /**
     * Checks if the given {@link CommandResponse} is an instance of {@link PingCommandResponse}.
     *
     * @param response The response which should be checked.
     *
     * @return True if the given response is an instance of {@link PingCommandResponse}.
     */
    @Override
    public boolean canHandle(CommandResponse response) {
        return response.hasPing();
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
        result.setResult(ResponseEntity.ok().build());
    }
}
