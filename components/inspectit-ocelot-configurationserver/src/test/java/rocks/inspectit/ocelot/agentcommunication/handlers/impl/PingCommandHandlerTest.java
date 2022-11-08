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
import rocks.inspectit.ocelot.grpc.CommandResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PingCommandHandlerTest {

    @InjectMocks
    PingCommandHandler pingCommandHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitServerSettings configuration;

    @Nested
    class HandleResponse {

        @Test
        public void handlesResponse() {
            CommandResponse response = CommandResponse.newBuilder().build();
            DeferredResult<ResponseEntity<?>> result = new DeferredResult<>();

            pingCommandHandler.handleResponse(response, result);

            assertThat(result.getResult()).isEqualTo(ResponseEntity.ok().build());
        }
    }

}
