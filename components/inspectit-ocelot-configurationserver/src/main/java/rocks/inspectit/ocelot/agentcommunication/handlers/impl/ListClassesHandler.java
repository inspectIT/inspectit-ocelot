package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.ListClassesCommandResponse;

import java.time.Duration;

@Slf4j
@Component
public class ListClassesHandler implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    @Override
    public boolean canHandle(Command command) {
        return command.hasListClasses();
    }

    @Override
    public boolean canHandle(CommandResponse response) {
        return response.hasListClasses();
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
        ListClassesCommandResponse classesResponse = response.getListClasses();
        result.setResult(ResponseEntity.ok().body(classesResponse.getResultList()));
    }
}
