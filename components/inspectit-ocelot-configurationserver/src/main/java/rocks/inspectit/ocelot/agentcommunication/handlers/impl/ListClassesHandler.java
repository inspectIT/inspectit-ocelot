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
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;

@Slf4j
@Component
public class ListClassesHandler implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    @Override
    public boolean canHandle(Command command) {
        return command instanceof ListClassesCommand;
    }

    @Override
    public boolean canHandle(CommandResponse response) {
        return response instanceof ListClassesCommand.Response;
    }

    @Override
    public DeferredResult<ResponseEntity<?>> prepareResponse(String agentId, Command command) {
        if (!canHandle(command)) {
            throw new IllegalArgumentException("ListClassesHandler can only handle commands of type ListClassesCommand.");
        }

        Duration responseTimeout = configuration.getAgentCommand().getResponseTimeout();
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(responseTimeout.toMillis());

        deferredResult.onTimeout(() -> ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build());

        return deferredResult;
    }

    @Override
    public void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result) {
        ListClassesCommand.Response classesResponse = (ListClassesCommand.Response) response;
        result.setResult(ResponseEntity.ok().body(classesResponse.getResult()));
    }
}
