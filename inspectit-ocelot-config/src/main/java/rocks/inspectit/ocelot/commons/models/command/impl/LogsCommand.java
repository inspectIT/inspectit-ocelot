package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

/**
 * Represents a Logs-Command. Logs commands are used to receive the logs from a certain agent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LogsCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "logs";

    private String logFormat = "%d{ISO8601} %-5p %-6r --- [inspectIT] [%15.15t] %-40.40logger{39} : %m%n%rEx";

    /**
     * Represents a response to the {@link rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand}.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {

        private String logs;

    }
}
