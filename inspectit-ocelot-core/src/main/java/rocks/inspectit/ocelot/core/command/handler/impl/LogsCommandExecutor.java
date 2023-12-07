package rocks.inspectit.ocelot.core.command.handler.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;

/**
 * Executor for executing {@link LogsCommand}s.
 */
@Component
public class LogsCommandExecutor implements CommandExecutor {

    @Autowired
    private LogPreloader logPreloader;

    /**
     * Checks if the given {@link Command} is an instance of {@link LogsCommand}.
     *
     * @param command The {@link Command} to be checked.
     *
     * @return True if the given {@link Command} is an instance of {@link LogsCommand}.
     */
    @Override
    public boolean canExecute(Command command) {
        return command instanceof LogsCommand;
    }

    /**
     * Executes the given {@link Command}. Throws an {@link IllegalArgumentException} if the given command is either null
     * or not handled by this implementation.
     *
     * @param command The command to be executed.
     *
     * @return An instance of {@link LogsCommand} with alive set to true and the id of the given command.
     */
    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }

        LogsCommand logsCommand = (LogsCommand) command;
        PatternLayout layout = new PatternLayout();
        layout.setPattern(logsCommand.getLogFormat());
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        layout.setContext(rootLogger.getLoggerContext());
        layout.start();

        LogsCommand.Response response = new LogsCommand.Response();
        response.setCommandId(command.getCommandId());
        StringBuilder logs = new StringBuilder();
        for (ILoggingEvent event : logPreloader.getPreloadedLogs()) {
            logs.append(layout.doLayout(event));
        }

        response.setLogs(logs.toString());
        layout.stop();
        return response;
    }
}
