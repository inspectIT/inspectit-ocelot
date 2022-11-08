package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse;
import rocks.inspectit.ocelot.grpc.ListClassesCommandResponse;

import java.time.Duration;

/**
 * Handler for the Agent Environment command.
 */
@Slf4j
@Component
public class EnvironmentCommandHandler  implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    @Override
    public boolean canHandle(Command command) {
        return command.hasEnvironment();
    }

    @Override
    public boolean canHandle(CommandResponse response) {
        return response.hasEnvironment();
    }

    @Override
    public void handleResponse(CommandResponse response, DeferredResult<ResponseEntity<?>> result) {


        EnvironmentCommandResponse environmentResponse = response.getEnvironment();
        try {
            // Because JsonFormat.printer().print() does not work with lists of messages, the detour using a JsonObject
            // is needed so only the list of TypeElements is returned as an answer,
            // and not a map with the list as the value for the key "result".
            String fullResponse = JsonFormat.printer().includingDefaultValueFields().print(environmentResponse);
            JsonObject obj = new JsonParser().parse(fullResponse).getAsJsonObject();
            result.setResult(ResponseEntity.ok().body(fullResponse));
        } catch (InvalidProtocolBufferException e) {
            log.error("Encountered exception when trying to build response to agent command '{}'", response.getCommandId(), e);
            result.setErrorResult(e);
        }
    }
}
