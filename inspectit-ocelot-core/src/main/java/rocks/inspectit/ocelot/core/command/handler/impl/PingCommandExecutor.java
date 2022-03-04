package rocks.inspectit.ocelot.core.command.handler.impl;

import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.PingCommand;
import rocks.inspectit.ocelot.grpc.PingCommandResponse;

/**
 * Executor for executing {@link PingCommand}s.
 */
@Component
public class PingCommandExecutor implements CommandExecutor {

    /**
     * Checks if the given {@link Command} is an instance of {@link PingCommand}.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the given {@link Command} is an instance of {@link PingCommand}.
     */
    @Override
    public boolean canExecute(Command command) {
        return command.hasPing();
    }

    /**
     * Executes the given {@link Command}. Throws an {@link IllegalArgumentException} if the given command is either null
     * or not handled by this implementation.
     *
     * @param command The command to be executed.
     *
     * @return An instance of {@link PingCommand} with alive set to true and the id of the given command.
     */
    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }

        return CommandResponse.newBuilder()
                .setCommandId(command.getCommandId())
                .setPing(PingCommandResponse.newBuilder())
                .build();
    }
}
