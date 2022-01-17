package rocks.inspectit.ocelot.commons.models.command;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;

import java.util.UUID;

/**
 * Represents a command to be executed by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = PingCommand.TYPE_IDENTIFIER, value = PingCommand.class),
        @JsonSubTypes.Type(name = ListClassesCommand.TYPE_IDENTIFIER, value = ListClassesCommand.class),
        @JsonSubTypes.Type(name = LogsCommand.TYPE_IDENTIFIER, value = LogsCommand.class)
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
