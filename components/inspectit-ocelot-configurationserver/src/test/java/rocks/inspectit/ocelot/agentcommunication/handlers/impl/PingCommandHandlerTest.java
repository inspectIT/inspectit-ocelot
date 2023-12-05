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
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PingCommandHandlerTest {

    @InjectMocks
    PingCommandHandler pingCommandHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitServerSettings configuration;

    @Nested
    class PrepareResponse {

        @Test
        public void cantHandleCommand() {
            Command command = null;
            String agentId = "test-id";

            try {
                pingCommandHandler.prepareResponse(agentId, command);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("PingCommandHandler can only handle commands of type PingCommand.");
            }

        }

        @Test
        public void preparesResponse() {
            when(configuration.getAgentCommand().getResponseTimeout()).thenReturn(Duration.ofMinutes(1));
            PingCommand command = new PingCommand();
            String agentId = "test-id";

            DeferredResult<ResponseEntity<?>> result = pingCommandHandler.prepareResponse(agentId, command);

            assertThat(result).isNotNull();

        }

    }

    @Nested
    class HandleResponse {

        @Test
        public void handlesResponse() {
            CommandResponse mockResponse = mock(CommandResponse.class);
            DeferredResult<ResponseEntity<?>> result = new DeferredResult<>();

            pingCommandHandler.handleResponse(mockResponse, result);

            assertThat(result.getResult()).isEqualTo(ResponseEntity.ok().build());
            verifyNoMoreInteractions(mockResponse);
        }
    }

}
