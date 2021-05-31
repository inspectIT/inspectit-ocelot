package rocks.inspectit.ocelot.core.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;

import java.util.List;

@Component
public class CommandDelegator {

    /**
     * List of implementations of {@link CommandExecutor}. This list is used to choose the matching {@link CommandExecutor}
     * for a given instance of {@link Command}.
     */
    @Autowired
    List<CommandExecutor> executors;

    /**
     * Delegates a given command to the responsible {@link CommandDelegator} and returns the respective return value.
     *
     * @param command The command to be executed.
     *
     * @return The return value of the command.
     */
    public CommandResponse delegate(Command command) {
        for(CommandExecutor executor: executors){
            if(executor.canExecute(command)) {
                return executor.execute(command);
            }
        }
        return null;
    }

}
