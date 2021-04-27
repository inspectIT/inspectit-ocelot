package rocks.inspectit.ocelot.commons.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.UUID;

/**
 * Represents a command to be executed by an inspecIT agent.
 */
@Data
@Getter
@AllArgsConstructor
public class AgentResponse {

    public static AgentResponse emptyResponse;

    public static AgentResponse getEmptyResponse() {
        if (emptyResponse == null) {
            emptyResponse = new AgentResponse(null, null);
        }
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
