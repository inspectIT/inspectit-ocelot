package rocks.inspectit.ocelot.core.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;

import java.util.List;

/**
 * This component redirects commands to the respective {@link CommandExecutor} instance.
 */
@Component
public class CommandDelegator {

    /**
     * List of implementations of {@link CommandExecutor}. This list is used to choose the matching {@link CommandExecutor}
     * for a given instance of {@link Command}.
     */
    @Autowired
    private List<CommandExecutor> executors;

    /**
     * Delegates a given command to the responsible {@link CommandDelegator} and returns the respective return value.
     *
     * @param command The command to be executed.
     *
     * @return The return value of the command.
     */
    public CommandResponse delegate(Command command) {
        for (CommandExecutor executor : executors) {
            if (executor.canExecute(command)) {
                return executor.execute(command);
            }
        }
        return null;
    }

}
