package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse;

/**
 * Handler for the Agent Environment command.
 */
@Slf4j
@Component
public class EnvironmentCommandHandler  implements CommandHandler {

    /**
     * Checks if the given {@link Command} is an instance of {@link rocks.inspectit.ocelot.grpc.EnvironmentCommand}.
     *
     * @param command The command which should be checked.
     * @return True if the given command is an instance of {@link rocks.inspectit.ocelot.grpc.EnvironmentCommand}.
     */
    @Override
    public boolean canHandle(Command command) {
        return command.hasEnvironment();
    }

    /**
     * Checks if the given {@link CommandResponse} is an instance of {@link rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse}.
     *
     * @param response The response which should be checked.
     * @return True if the given response is an instance of {@link rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse}.
     */
    @Override
    public boolean canHandle(CommandResponse response) {
        return response.hasEnvironment();
    }

    /**
     * Takes an instance of {@link CommandResponse} as well as an instance of {@link DeferredResult}.
     * Sets the {@link ResponseEntity} of the {@link DeferredResult} according to the
     * Environment Information received from the respective Agent.
     *
     * @param response The {@link CommandResponse} to be handled.
     * @param result   The {@link DeferredResult} the response should be written in.
     */
    @Override
    public void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result) {

        EnvironmentCommandResponse environmentResponse = response.getEnvironment();
        try {
            String fullResponse = JsonFormat.printer().includingDefaultValueFields().print(environmentResponse);
            result.setResult(ResponseEntity.ok().body(fullResponse));
        } catch (InvalidProtocolBufferException e) {
            log.error("Encountered exception when trying to build response to agent command '{}'", response.getCommandId(), e);
            result.setErrorResult(e);
        }
    }
}
