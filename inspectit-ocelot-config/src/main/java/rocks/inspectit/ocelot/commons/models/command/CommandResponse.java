package rocks.inspectit.ocelot.commons.models.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.LogsCommand;
import rocks.inspectit.ocelot.commons.models.command.impl.PingCommand;

import java.util.UUID;

/**
 * Represents a response to be send by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = PingCommand.TYPE_IDENTIFIER, value = PingCommand.Response.class),
        @JsonSubTypes.Type(name = ListClassesCommand.TYPE_IDENTIFIER, value = ListClassesCommand.Response.class),
        @JsonSubTypes.Type(name = LogsCommand.TYPE_IDENTIFIER, value = LogsCommand.Response.class),
})
public abstract class CommandResponse {

    /**
     * The id of the command this response belongs to.
     */
    private UUID commandId;
}
