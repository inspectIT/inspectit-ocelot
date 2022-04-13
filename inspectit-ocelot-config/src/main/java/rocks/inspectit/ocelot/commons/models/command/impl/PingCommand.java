package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import java.util.UUID;

/**
 * Represents a Ping-Command. Ping commands are used to check if the receiving agent does exist.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "ping";

    /**
     * Represents a response to the {@link rocks.inspectit.ocelot.commons.models.command.impl.PingCommand}.
     */
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {

        public Response(UUID commandId) {
            super(commandId);
        }
    }
}
