package rocks.inspectit.ocelot.commons.models.command;

import lombok.*;
import rocks.inspectit.ocelot.commons.models.command.impl.EmptyCommand;

import java.util.UUID;

/**
 * Represents a command to be executed by an inspectIT agent.
 */
@Data
public abstract class Command {

    /**
     * The id of this command.
     */
    private UUID commandId;

    /**
     * Basic constructor which creates a UUID for this command.
     */
    public Command() {
        this.commandId = UUID.randomUUID();
    }

}
