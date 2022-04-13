package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.ListDependenciesCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Executor for executing {@link ListDependenciesCommand}s.
 */
@Slf4j
@Component
public class ListDependenciesCommandExecutor implements CommandExecutor {

    /**
     * Checks if the given {@link Command} is an instance of {@link ListDependenciesCommand}.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the given {@link Command} is an instance of {@link ListDependenciesCommand}.
     */
    @Override
    public boolean canExecute(Command command) {
        return command instanceof ListDependenciesCommand;
    }

    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }

        ListDependenciesCommand ldCommand = (ListDependenciesCommand) command;

        log.debug("Executing a ListDependenciesCommand: {}", ldCommand.getCommandId().toString());

        Set<Class<?>> setCopy = new HashSet<>(discoveryService.getKnownClasses()); //Something like the discService but for the dependencies still needs to be implemented in inspecitit-core
        ListDependenciesCommand.Response.DependecyElement[] result = setCopy.parallelStream().map(dependenzzy -> {
            try {
                ListDependenciesCommand.Response.DependecyElement element = new ListDependenciesCommand.Response.DependecyElement();
                element.setName("Test Name"); //to do
                element.setVersion("Test Version");// to do
                return element;
            } catch (Throwable e) {
                log.debug("Could not add dependency to result list: {}", dependenzzy);
                return null;
            }
        }).filter(Objects::nonNull).toArray(ListDependenciesCommand.Response.DependecyElement[]::new);

        log.debug("Finished executing ListDependenciesCommand: {}", ldCommand.getCommandId().toString());

        ListDependenciesCommand.Response response = new ListDependenciesCommand.Response();
        response.setCommandId(ldCommand.getCommandId());
        response.setResult(result);
        return response;
    }
}
