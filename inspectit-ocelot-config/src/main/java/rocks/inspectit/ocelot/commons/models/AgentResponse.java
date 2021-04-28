package rocks.inspectit.ocelot.commons.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a command to be executed by an inspectIT agent.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentResponse {

    public static final AgentResponse emptyResponse = new AgentResponse(null, null);

    public static AgentResponse getEmptyResponse() {
        return emptyResponse;
    }

    /**
     * The id of this command.
     */
    private UUID commandId;

    /**
     * The payload this command returned.
     */
    private Object payload;

}
