package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.ListClassesCommand;
import rocks.inspectit.ocelot.grpc.PingCommand;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PingCommandExecutorTest {

    @InjectMocks
    private PingCommandExecutor executor;

    @Nested
    public class CanExecute {

        @Test
        public void nonPingCommand() {
            boolean result = executor.canExecute(Command.newBuilder()
                    .setListClasses(ListClassesCommand.newBuilder())
                    .build());

            assertFalse(result);
        }

        @Test
        public void pingCommand() {
            boolean result = executor.canExecute(Command.newBuilder().setPing(PingCommand.newBuilder()).build());

            assertTrue(result);
        }
    }

    @Nested
    public class execute {

        @Test
        public void executes() {
            Command command = Command.newBuilder()
                    .setPing(PingCommand.newBuilder())
                    .setCommandId(UUID.randomUUID().toString())
                    .build();

            CommandResponse response = executor.execute(command);

            assertNotNull(response);
            assertEquals(response.getCommandId(), command.getCommandId());
        }
    }
}
