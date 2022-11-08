package rocks.inspectit.ocelot.core.command.handler.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.grpc.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCommandExecutorTest {

    @InjectMocks
    private EnvironmentCommandExecutor executor;

    @Nested
    public class CanExecute {

        @Test
        public void nonEnvironmentCommand() {
            boolean result = executor.canExecute(Command.newBuilder()
                    .setListClasses(ListClassesCommand.newBuilder())
                    .build());

            assertThat(result).isFalse();
        }

        @Test
        public void environmentCommand() {
            boolean result = executor.canExecute(Command.newBuilder()
                    .setEnvironment(EnvironmentCommand.newBuilder())
                    .build());

            assertThat(result).isTrue();
        }
    }

    @Nested
    public class Execute {

        private Command environmentCommand;

        @BeforeEach
        public void createEnvironmentCommand() {
            environmentCommand = Command.newBuilder()
                    .setEnvironment(EnvironmentCommand.newBuilder())
                    .setCommandId(UUID.randomUUID().toString())
                    .build();
        }

        @Test
        public void failsOnNonEnvironmentCommand() {
            Command nonEnvironmentCommand = Command.newBuilder()
                    .setPing(PingCommand.newBuilder())
                    .setCommandId(UUID.randomUUID().toString())
                    .build();

            assertThatThrownBy(() -> {
                executor.execute(nonEnvironmentCommand);
            }).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        public void returnsCommandIdFromCommand() {
            CommandResponse response = executor.execute(environmentCommand);

            assertThat(response.getCommandId()).isEqualTo(environmentCommand.getCommandId());
        }

        @Test
        public void returnsEnvironmentVariables() {
            EnvironmentCommandResponse environmentCommandResponse = executor.execute(environmentCommand)
                    .getEnvironment();

            assertThat(environmentCommandResponse.getEnvironmentVariablesMap()).containsKey("PATH");
        }

        @Test
        public void returnsSystemProperties() {
            EnvironmentCommandResponse environmentCommandResponse = executor.execute(environmentCommand)
                    .getEnvironment();

            assertThat(environmentCommandResponse.getSystemPropertiesMap()).containsKey("os.name");
        }

        @Test
        public void returnsJvmArguments() {
            EnvironmentCommandResponse environmentCommandResponse = executor.execute(environmentCommand)
                    .getEnvironment();

            assertThat(environmentCommandResponse.getJvmArgumentsList()).isNotEmpty();
        }
    }
}
