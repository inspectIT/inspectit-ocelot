package rocks.inspectit.ocelot.core.command.handler.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.LogsCommand;
import rocks.inspectit.ocelot.grpc.LogsCommandResponse;

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
        return command.hasLogs();
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

        LogsCommand logsCommand = command.getLogs();
        PatternLayout layout = new PatternLayout();
        layout.setPattern(logsCommand.getLogFormat());
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        layout.setContext(rootLogger.getLoggerContext());
        layout.start();

        StringBuilder logs = new StringBuilder();
        for (ILoggingEvent event : logPreloader.getPreloadedLogs()) {
            logs.append(layout.doLayout(event));
        }

        CommandResponse response = CommandResponse.newBuilder()
                .setCommandId(command.getCommandId())
                .setLogs(LogsCommandResponse.newBuilder().setLogs(logs.toString()))
                .build();

        layout.stop();

        return CommandResponse.newBuilder()
                .setCommandId(command.getCommandId())
                .setLogs(LogsCommandResponse.newBuilder().setLogs(logs.toString()))
                .build();
    }
}
