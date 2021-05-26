package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.response.impl.PingResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PingCommandExecutorTest {

    @InjectMocks
    private PingCommandExecutor executor;


    @Nested
    public class CanExecute {

        private class NonPingCommand extends Command {

        }

        @Test
        public void nullParam(){
            boolean result = executor.canExecute(null);

            assertFalse(result);
        }

        @Test
        public void nonPingCommand(){
            boolean result = executor.canExecute(new NonPingCommand());

            assertFalse(result);
        }

        @Test
        public void pingCommand(){
            boolean result = executor.canExecute(new PingCommand());

            assertTrue(result);
        }
    }

    @Nested
    public class execute {

        @Test
        public void executes(){
            PingCommand command = new PingCommand();

            CommandResponse response = executor.execute(command);

            assertTrue(response instanceof PingResponse);
            assertEquals(response.getCommandId(), command.getCommandId());
        }
    }
}
