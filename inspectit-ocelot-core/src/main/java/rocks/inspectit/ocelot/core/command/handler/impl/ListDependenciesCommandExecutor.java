package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.ListDependenciesCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;

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

        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String classPath = bean.getClassPath();//    /opt/spring-boot/app.jar

        System.out.println(Arrays.toString(classPath.split(":")));

        log.debug("Finished executing ListDependenciesCommand: {}", ldCommand.getCommandId().toString());

        ListDependenciesCommand.Response response = new ListDependenciesCommand.Response();
        response.setCommandId(ldCommand.getCommandId());
        //response.setResult(result);
        return response;
    }
}
