package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PingCommandExecutorTest {

    @InjectMocks
    PingCommandExecutor executor;


    @Nested
    class CanExecute {

        private class NonPingCommand extends Command {}

        @Test
        void nullParam() {
            boolean result = executor.canExecute(null);

            assertFalse(result);
        }

        @Test
        void nonPingCommand() {
            boolean result = executor.canExecute(new NonPingCommand());

            assertFalse(result);
        }

        @Test
        void pingCommand() {
            boolean result = executor.canExecute(new PingCommand());

            assertTrue(result);
        }
    }

    @Nested
    class execute {

        @Test
        void executes() {
            PingCommand command = new PingCommand();

            CommandResponse response = executor.execute(command);

            assertTrue(response instanceof PingCommand.Response);
            assertEquals(response.getCommandId(), command.getCommandId());
        }
    }
}
