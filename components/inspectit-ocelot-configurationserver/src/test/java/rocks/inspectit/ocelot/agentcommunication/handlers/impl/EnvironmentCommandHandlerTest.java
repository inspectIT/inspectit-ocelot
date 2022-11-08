package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.PingCommand;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCommandHandlerTest {

    @InjectMocks
    EnvironmentCommandHandler environmentCommandHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitServerSettings configuration;
// TODO
//    @Nested
//    class PrepareResponse {
//
//        @Test
//        public void cantHandleCommand() {
//            Command command = null;
//            String agentId = "test-id";
//
//            try {
//                environmentCommandHandler.canHandle(PingCommand.newBuilder().build());
//            } catch (IllegalArgumentException e) {
//                assertThat(e.getMessage()).isEqualTo("EnvironmentCommandHandler can only handle commands of type EnvironmentCommand.");
//            }
//
//        }
//
//        @Test
//        public void preparesResponse() {
//            when(configuration.getAgentCommand().getResponseTimeout()).thenReturn(Duration.ofMinutes(1));
//            EnvironmentCommand command = new EnvironmentCommand();
//            String agentId = "test-id";
//
//            DeferredResult<ResponseEntity<?>> result = environmentCommandHandler.prepareResponse(agentId, command);
//
//            assertThat(result).isNotNull();
//
//        }
//
//    }
//
//    @Nested
//    class HandleResponse {
//
//        @Test
//        public void handlesResponse() {
//            EnvironmentCommand.Response mockResponse = mock(EnvironmentCommand.Response.class);
//            DeferredResult<ResponseEntity<?>> result = new DeferredResult<>();
//
//            environmentCommandHandler.handleResponse(mockResponse, result);
//
//            assertThat(result.getResult()).isEqualTo(ResponseEntity.ok().build());
//        }
//    }

}
