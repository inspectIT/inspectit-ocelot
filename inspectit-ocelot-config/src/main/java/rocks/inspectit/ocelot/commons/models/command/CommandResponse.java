package rocks.inspectit.ocelot.commons.models.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.impl.*;

import java.util.UUID;

/**
 * Represents a response to be sent by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = PingCommand.TYPE_IDENTIFIER, value = PingCommand.Response.class),
        @JsonSubTypes.Type(name = ListClassesCommand.TYPE_IDENTIFIER, value = ListClassesCommand.Response.class),
        @JsonSubTypes.Type(name = LogsCommand.TYPE_IDENTIFIER, value = LogsCommand.Response.class),
        @JsonSubTypes.Type(name = EnvironmentCommand.TYPE_IDENTIFIER, value = EnvironmentCommand.Response.class),
        @JsonSubTypes.Type(name = InstrumentationFeedbackCommand.TYPE_IDENTIFIER, value = InstrumentationFeedbackCommand.Response.class),
})
public abstract class CommandResponse {

    /**
     * The id of the command this response belongs to.
     */
    private UUID commandId;
}
