package rocks.inspectit.ocelot.core.command.handler.impl;

import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.EnvironmentCommandResponse;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for executing {@link rocks.inspectit.ocelot.grpc.EnvironmentCommand}s.
 */
@Component
public class EnvironmentCommandExecutor implements CommandExecutor {

    @Override
    public boolean canExecute(Command command) {
        return command.hasEnvironment();
    }

    @Override
    public CommandResponse execute(Command command) {
        if (!canExecute(command)) {
            String exceptionMessage = "Invalid command type. Executor does not support commands of type " + command.getClass();
            throw new IllegalArgumentException(exceptionMessage);
        }
        return CommandResponse
                .newBuilder()
                .setCommandId(command.getCommandId())
                .setEnvironment(EnvironmentCommandResponse
                        .newBuilder()
                        .putAllEnvironmentVariables(System.getenv())
                        .putAllSystemProperties(propertiesAsMap())
                        .addAllJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments())
                )
                .build();
    }

    private Map<String, String> propertiesAsMap() {
        Map<String, String> propertiesAsMap = new HashMap<>();
        for (String propertyName : System.getProperties().stringPropertyNames()) {
            propertiesAsMap.put(propertyName, System.getProperty(propertyName));
        }
        return propertiesAsMap;
    }
}
