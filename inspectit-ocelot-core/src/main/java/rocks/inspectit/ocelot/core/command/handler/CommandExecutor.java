package rocks.inspectit.ocelot.core.command.handler;

import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;

/**
 * Interface for all command executors. These executors contain the logic how agent commands should be processed.
 */
public interface CommandExecutor {

    /**
     * Checks if the given {@link Command} can be executed by this implementation of CommandExecutor.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the command can be executed by this executor.
     */
    boolean canExecute(Command command);

    /**
     * Executes the given {@link Command} and returns the respective {@link CommandResponse}.
     *
     * @param command The command to be executed.
     *
     * @return The respective {@link CommandResponse}.
     */
    CommandResponse execute(Command command);
}
