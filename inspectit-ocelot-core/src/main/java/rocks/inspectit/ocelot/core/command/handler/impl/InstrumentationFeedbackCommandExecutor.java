package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.instrumentation.hook.HookManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executor for executing {@link InstrumentationFeedbackCommand}s.
 */
@Slf4j
@Component
public class InstrumentationFeedbackCommandExecutor implements CommandExecutor {

    @Autowired
    private HookManager hookManager;

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

        Map<Class<?>, Map<String, MethodHook>> activeHooks = hookManager.getHooks();
        InstrumentationFeedbackCommand.Response response = new InstrumentationFeedbackCommand.Response();
        response.setCommandId(command.getCommandId());

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> instrumentationFeedback = new HashMap<>();

        activeHooks.forEach((clazz, methodHookMap) -> {
            InstrumentationFeedbackCommand.ClassInstrumentation classInstrumentation = resolveClassInstrumentation(methodHookMap);
            instrumentationFeedback.put(clazz.getName(), classInstrumentation);
        });

        response.setInstrumentationFeedback(instrumentationFeedback);

        return response;
    }

    /**
     * @param methodHookMap the map of method signatures and {@link MethodHook}s
     *
     * @return the resolved {@link InstrumentationFeedbackCommand.ClassInstrumentation}
     */
    private InstrumentationFeedbackCommand.ClassInstrumentation resolveClassInstrumentation(Map<String, MethodHook> methodHookMap) {
        Map<String, List<String>> methodInstrumentationFeedback = new HashMap<>();

        methodHookMap.forEach((method, methodHook) -> {
            List<String> rules = methodHook.getSourceConfiguration().getMatchedRulesNames();

            methodInstrumentationFeedback.put(method, rules);
        });

        return new InstrumentationFeedbackCommand.ClassInstrumentation(methodInstrumentationFeedback);
    }
}
