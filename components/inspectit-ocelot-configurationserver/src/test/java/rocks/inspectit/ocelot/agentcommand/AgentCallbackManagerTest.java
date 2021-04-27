package rocks.inspectit.ocelot.agentcommand;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
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
        public void addCallbackCommand() throws ExecutionException {
            Thread mockThread = mock(Thread.class);
            String agentId = "test-agent";
            UUID id = UUID.randomUUID();
            String expectedId = agentId + id.toString();

            agentCallbackManager.addCallbackCommand(agentId, id, mockThread);

            Map<String, LinkedList<Thread>> agentCallBackMap = agentCallbackManager.agentCallBackMap.asMap();
            assertThat(agentCallBackMap).hasSize(1);
            LinkedList<Thread> list = agentCallBackMap.get(expectedId);
            assertThat(list).hasSize(1);
            assertThat(list).contains(mockThread);
        }

        @Test
        public void ignoresNullParams() throws ExecutionException {
            Thread mockThread = mock(Thread.class);

            agentCallbackManager.addCallbackCommand(null, null, mockThread);

            Map<String, LinkedList<Thread>> agentCallBackMap = agentCallbackManager.agentCallBackMap.asMap();
            assertThat(agentCallBackMap).hasSize(0);
        }
    }

    @Nested
    class RunNextCommandWithId {

        @Test
        public void hasCommand() throws ExecutionException {
            Thread mockCommandThread = mock(Thread.class);
            UUID id = UUID.randomUUID();
            agentCallbackManager.agentCallBackMap.get(id.toString()).push(mockCommandThread);

            agentCallbackManager.runNextCommandWithId("", id);

            //Verify that the command was started.
            verify(mockCommandThread).start();

            //Verify that the command was removed from the map.
            Map<String, LinkedList<Thread>> map = agentCallbackManager.agentCallBackMap.asMap();
            assertThat(map).isEmpty();
        }

        @Test
        public void ignoresNullParams() throws ExecutionException {
            agentCallbackManager.agentCallBackMap = mock(LoadingCache.class);

            agentCallbackManager.runNextCommandWithId(null, null);

            verifyZeroInteractions(agentCallbackManager.agentCallBackMap);
        }

    }
}
