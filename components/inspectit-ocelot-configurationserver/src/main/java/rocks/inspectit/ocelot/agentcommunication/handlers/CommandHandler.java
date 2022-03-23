package rocks.inspectit.ocelot.agentcommunication.handlers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;

public interface CommandHandler {

    /**
     * Checks if this handler handles a given command. Returns true if this is the case.
     *
     * @param command The command which should be checked.
     *
     * @return True if the command is handled by this handler.
     */
    boolean canHandle(Command command);

    /**
     * Checks if this handler handles a given CommandResponse, Returns true if this is the case.
     *
     * @param response The response which should be checked.
     *
     * @return True if the response is handled by this handler.
     */
    boolean canHandle(CommandResponse response);

    /**
     * Takes an instance of {@link CommandResponse} as well as an instance of {@link DeferredResult} and handles
     * the given parameters as implemented by the handler.
     *
     * @param response The {@link CommandResponse} to be handled.
     * @param result   The {@link DeferredResult} the response should be written in.
     */
    void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result);
}
