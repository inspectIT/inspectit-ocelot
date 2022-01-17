package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.*;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import java.util.UUID;

/**
 * Represents a Logs-Command. Logs commands are used to recieve the logs from a certain agent.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LogsCommand extends Command {
    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "logs";

    /**
     * Represents a response to the {@link rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand}.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {
        private String result;
    }
}
