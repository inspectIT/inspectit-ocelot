package rocks.inspectit.ocelot.commons.models.command.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a response to be send by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class CommandResponse {

    /**
     * The id of the command this response belongs to.
     */
    private UUID commandId;
}
