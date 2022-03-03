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
     * Takes an instance of {@link Command} as well as a String resembling the id of the agent the command is meant for.
     * Prepares an instance of {@link DeferredResult} for this command.
     *
     * @param agentId The id of the agent the command is meant for.
     * @param command The command to be Executed.
     *
     * @return An instance of {@link DeferredResult} which is prepared as defined by the handler.
     */
    DeferredResult<ResponseEntity<?>> prepareResponse(String agentId, Command command);

    /**
     * Takes an instance of {@link CommandResponse} as well as an instance of {@link DeferredResult} and handles
     * the given parameters as implemented by the handler.
     *
     * @param response The {@link CommandResponse} to be handled.
     * @param result   The {@link DeferredResult} the response should be written in.
     */
    void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result);
}
