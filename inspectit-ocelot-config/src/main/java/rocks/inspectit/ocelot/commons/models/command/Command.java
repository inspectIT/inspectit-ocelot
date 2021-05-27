package rocks.inspectit.ocelot.commons.models.command;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;

import java.util.UUID;

/**
 * Represents a command to be executed by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "ping-command", value = PingCommand.class),
})
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
