package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCommandHandlerTest {

    @InjectMocks
    EnvironmentCommandHandler environmentCommandHandler;

    @Nested
    class HandleResponse {

        @Test
        public void handlesResponse() {
            EnvironmentCommandResponse response = EnvironmentCommandResponse
                    .newBuilder()
                    .putSystemProperties("key", "value")
                    .build();

            DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>();

            environmentCommandHandler.handleResponse(CommandResponse.newBuilder()
                    .setEnvironment(response)
                    .build(), deferredResult);

            ResponseEntity<String> result = (ResponseEntity<String>) deferredResult.getResult();
            JsonObject obj = new JsonParser().parse(result.getBody()).getAsJsonObject();

            assertThat(obj.get("systemProperties").getAsJsonObject().get("key").getAsString()).isEqualTo("value");
        }

        @Test
        public void handlesResponseReturnsOK() {
            EnvironmentCommandResponse response = EnvironmentCommandResponse
                    .newBuilder()
                    .putSystemProperties("key", "value")
                    .build();

            DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>();

            environmentCommandHandler.handleResponse(CommandResponse.newBuilder()
                    .setEnvironment(response)
                    .build(), deferredResult);

            ResponseEntity<String> result = (ResponseEntity<String>) deferredResult.getResult();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
