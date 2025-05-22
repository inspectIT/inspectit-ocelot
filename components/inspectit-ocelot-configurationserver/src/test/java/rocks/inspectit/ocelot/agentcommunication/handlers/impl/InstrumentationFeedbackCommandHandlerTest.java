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
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InstrumentationFeedbackCommandHandlerTest {

    @InjectMocks
    InstrumentationFeedbackCommandHandler handler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitServerSettings configuration;

    @Nested
    class PrepareResponse {

        @Test
        public void cantHandleCommand() {
            Command command = null;
            String agentId = "test-id";

            try {
                handler.prepareResponse(agentId, command);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("InstrumentationFeedbackCommandHandler can only handle commands of type InstrumentationFeedbackCommand");
            }

        }

        @Test
        public void preparesResponse() {
            when(configuration.getAgentCommand().getResponseTimeout()).thenReturn(Duration.ofMinutes(1));
            InstrumentationFeedbackCommand command = new InstrumentationFeedbackCommand();
            String agentId = "test-id";

            DeferredResult<ResponseEntity<?>> result = handler.prepareResponse(agentId, command);

            assertThat(result).isNotNull();

        }
    }

    @Nested
    class HandleResponse {

        @Test
        public void handlesResponse() {
            InstrumentationFeedbackCommand.Response mockResponse = mock(InstrumentationFeedbackCommand.Response.class);
            DeferredResult<ResponseEntity<?>> result = new DeferredResult<>();

            handler.handleResponse(mockResponse, result);

            assertThat(result.getResult()).isEqualTo(ResponseEntity.ok().body(Collections.emptyMap()));
        }
    }
}
