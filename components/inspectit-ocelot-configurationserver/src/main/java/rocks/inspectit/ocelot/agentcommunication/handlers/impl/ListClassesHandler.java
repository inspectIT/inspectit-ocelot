package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import com.googlecode.protobuf.format.JsonFormat;
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

        // TODO: 09.03.2022 Either get 'result' key from json in UI or parse the String and extract only result part.
        // Just setting printToString(classesResponse.getResultList()) sadly does not work.
        result.setResult(ResponseEntity.ok().body(new JsonFormat().printToString(classesResponse)));
    }
}
