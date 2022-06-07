package rocks.inspectit.ocelot.commons.models.command.impl;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Represents an Environment-Command. Environment commands are used to receive the details of a certain agent.
 * These details include environment variables, system properties and JVM arguments.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnvironmentCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "environment";

    /**
     * Represents a response to the {@link EnvironmentCommand}.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {
        private EnvironmentDetail environment;
    }

    /**
     * Represents the structure of the returned environment details
     */
    @Data
    public static class EnvironmentDetail {
        @JsonPropertyOrder(alphabetic = true)
        private Map<String, String> environmentVariables;
        @JsonPropertyOrder(alphabetic = true)
        private Properties systemProperties;
        private List<String> jvmArguments;
    }
}
