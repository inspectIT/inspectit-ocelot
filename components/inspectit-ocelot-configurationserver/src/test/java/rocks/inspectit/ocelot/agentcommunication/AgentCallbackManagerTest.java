package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.CommandResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentCallbackManagerTest {

    @InjectMocks
    AgentCallbackManager agentCallbackManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitServerSettings configuration;

    @BeforeEach
    public void beforeEach() {
        when(configuration.getAgentCommand().getResponseTimeout()).thenReturn(Duration.ofSeconds(5));

        agentCallbackManager.postConstruct();
    }

    @Nested
    class AddCallbackCommand {

        @Test
        public void addCallbackCommand() {
            UUID id = UUID.randomUUID();
            DeferredResult<ResponseEntity<?>> testResult = new DeferredResult<>();

            agentCallbackManager.addCommandCallback(id, testResult);

            Map<UUID, DeferredResult<ResponseEntity<?>>> resultCacheMap = agentCallbackManager.resultCache.asMap();
            assertThat(resultCacheMap).containsOnlyKeys(id);
            assertThat(resultCacheMap.get(id)).isEqualTo(testResult);
        }

        @Test
        public void ignoresNullParams() {
            UUID id = UUID.randomUUID();

            agentCallbackManager.addCommandCallback(id, null);

            Map<UUID, DeferredResult<ResponseEntity<?>>> agentCallBackMap = agentCallbackManager.resultCache.asMap();
            assertThat(agentCallBackMap).hasSize(0);
        }

        @Test
        public void throwsExceptionOnNullId() {
            DeferredResult<ResponseEntity<?>> testResult = new DeferredResult<>();

            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> agentCallbackManager.addCommandCallback(null, testResult))
                    .withMessage("The given command id must not be null!");
        }
    }

    @Nested
    class RunNextCommandWithId {

        @Test
        public void hasCommand() {
            DeferredResult<ResponseEntity<?>> testResult = new DeferredResult<>();
            UUID id = UUID.randomUUID();
            agentCallbackManager.resultCache.put(id, testResult);
            CommandResponse response = CommandResponse.newBuilder().build();
            CommandHandler mockHandler = mock(CommandHandler.class);
            when(mockHandler.canHandle(response)).thenReturn(true);
            List<CommandHandler> handlerList = new ArrayList<>();
            handlerList.add(mockHandler);
            agentCallbackManager.handlers = handlerList;

            agentCallbackManager.handleCommandResponse(id, response);

            //Verify that the command was removed from the map.
            Map<UUID, DeferredResult<ResponseEntity<?>>> map = agentCallbackManager.resultCache.asMap();
            assertThat(map).isEmpty();
            verify(mockHandler).handleResponse(response, testResult);
        }

        @Test
        public void commandIdNull() {
            agentCallbackManager.resultCache = mock(LoadingCache.class);

            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> agentCallbackManager.handleCommandResponse(null, null))
                    .withMessage("The given command id must not be null!");
        }
    }
}
