package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;

/**
 * Handler for the Agent instrumentation feedback command
 */
@Slf4j
@Component
public class InstrumentationFeedbackCommandHandler implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    /**
     * Checks if the given {@link Command} is an instance of {@link InstrumentationFeedbackCommand}
     *
     * @param command The command which should be checked
     *
     * @return True if the given command is an instance of {@link InstrumentationFeedbackCommand}
     */
    @Override
    public boolean canHandle(Command command) {
        return command instanceof InstrumentationFeedbackCommand;
    }

    /**
     * Checks if the given {@link CommandResponse} is an instance of {@link InstrumentationFeedbackCommand.Response}
     *
     * @param response The response which should be checked
     *
     * @return True if the given response is an instance of {@link InstrumentationFeedbackCommand.Response}
     */
    @Override
    public boolean canHandle(CommandResponse response) {
        return response instanceof InstrumentationFeedbackCommand.Response;
    }

    @Override
    public DeferredResult<ResponseEntity<?>> prepareResponse(String agentId, Command command) {
        if (!canHandle(command)) {
            throw new IllegalArgumentException("InstrumentationFeedbackCommandHandler can only handle commands of type InstrumentationFeedbackCommand");
        }

        Duration responseTimeout = configuration.getAgentCommand().getResponseTimeout();
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(responseTimeout.toMillis());
        deferredResult.onTimeout(() -> ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build());

        return deferredResult;
    }

    /**
     * Takes an instance of {@link CommandResponse} as well as an instance of {@link DeferredResult}. Sets the
     * {@link ResponseEntity} of the {@link DeferredResult} to the status OK. In this handler the given response is
     * ignored since the response itself indicates that the agent is alive
     *
     * @param response The {@link CommandResponse} to be handled
     * @param result   The {@link DeferredResult} the response should be written in
     */
    @Override
    public void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result) {
        InstrumentationFeedbackCommand.Response feedbackResponse = (InstrumentationFeedbackCommand.Response) response;
        result.setResult(ResponseEntity.ok().body(feedbackResponse.getInstrumentationFeedback()));
    }
}
