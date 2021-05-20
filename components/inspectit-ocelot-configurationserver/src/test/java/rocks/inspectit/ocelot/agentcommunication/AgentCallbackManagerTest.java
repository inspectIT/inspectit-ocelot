package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.agentcommunication.handlers.CommandHandler;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentCallbackManagerTest {

    @InjectMocks
    AgentCallbackManager agentCallbackManager;

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

            try {
                agentCallbackManager.addCommandCallback(null, testResult);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("The given command id may never be null!");
            }
        }
    }

    @Nested
    class RunNextCommandWithId {

        @Test
        public void hasCommand() throws ExecutionException {
            DeferredResult<ResponseEntity<?>> testResult = new DeferredResult<>();
            UUID id = UUID.randomUUID();
            agentCallbackManager.resultCache.put(id, testResult);
            CommandResponse response = mock(CommandResponse.class);
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
        public void commandIdNull() throws ExecutionException {
            agentCallbackManager.resultCache = mock(LoadingCache.class);

            try {
                agentCallbackManager.handleCommandResponse(null, null);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("The given command id may never be null!");
            }
            verifyZeroInteractions(agentCallbackManager.resultCache);
        }
    }
}
