package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.selfmonitoring.instrumentation.InstrumentationFeedbackService;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for executing {@link InstrumentationFeedbackCommand}s.
 */
@Slf4j
@Component
public class InstrumentationFeedbackCommandExecutor implements CommandExecutor {

    @Autowired
    private InstrumentationFeedbackService service;

    /**
     * Checks if the given {@link Command} is an instance of {@link InstrumentationFeedbackCommand}.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the given {@link Command} is an instance of {@link InstrumentationFeedbackCommand}
     */
    @Override
    public boolean canExecute(Command command) {
        return command instanceof InstrumentationFeedbackCommand;
    }

    /**
     * Executes the given {@link Command}. Throws an {@link IllegalArgumentException} if the given command is either null
     * or not handled by this implementation.
     *
     * @param command The command to be executed.
     *
     * @return An instance of {@link InstrumentationFeedbackCommand} with alive set to true and the id of the given command
     */
    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }

        log.debug("Executing InstrumentationFeedbackCommand {}", command.getCommandId().toString());

        InstrumentationFeedbackCommand.Response response = new InstrumentationFeedbackCommand.Response();
        response.setCommandId(command.getCommandId());

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> instrumentationFeedback = service.getInstrumentation();
        response.setInstrumentationFeedback(instrumentationFeedback);

        return response;
    }

}
