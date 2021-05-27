package rocks.inspectit.ocelot.commons.models.command.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.response.impl.PingResponse;

import java.util.UUID;

/**
 * Represents a response to be send by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "ping", value = PingResponse.class),
})
public abstract class CommandResponse {

    /**
     * The id of the command this response belongs to.
     */
    private UUID commandId;
}
