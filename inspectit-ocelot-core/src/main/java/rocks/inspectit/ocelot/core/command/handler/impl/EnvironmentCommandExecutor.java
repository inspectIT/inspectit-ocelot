package rocks.inspectit.ocelot.core.command.handler.impl;

import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;

import java.lang.management.ManagementFactory;

/**
 * Executor for executing {@link EnvironmentCommand}s.
 */
@Component
public class EnvironmentCommandExecutor implements CommandExecutor {

    /**
     * Checks if the given {@link Command} is an instance of {@link EnvironmentCommand}.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the given {@link Command} is an instance of {@link EnvironmentCommand}.
     */
    @Override
    public boolean canExecute(Command command) {
        return command instanceof EnvironmentCommand;
    }

    /**
     * Executes the given {@link Command}. Throws an {@link IllegalArgumentException} if the given command is either null
     * or not handled by this implementation.
     *
     * @param command The command to be executed.
     *
     * @return An instance of {@link EnvironmentCommand} with alive set to true and the id of the given command.
     */
    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }

        EnvironmentCommand.Response response = new EnvironmentCommand.Response();
        response.setCommandId(command.getCommandId());

        EnvironmentCommand.EnvironmentDetail body = new EnvironmentCommand.EnvironmentDetail();
        body.setEnvironmentVariables(System.getenv());
        body.setSystemProperties(System.getProperties());
        body.setJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments());

        response.setEnvironment(body);

        return response;
    }
}
