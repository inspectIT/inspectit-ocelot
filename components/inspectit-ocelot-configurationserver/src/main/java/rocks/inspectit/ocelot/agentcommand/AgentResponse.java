package rocks.inspectit.ocelot.agentcommand;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a command to be executed by an inspectIT agent.
 */
@Data
@Getter
public class AgentResponse {

    public static AgentResponse emptyResponse;

    public static AgentResponse getEmptyResponse() {
        if (emptyResponse == null) {
            emptyResponse = new AgentResponse(null, null);
        }
        return emptyResponse;
    }

    public AgentResponse(@JsonProperty("commandId") UUID commandId, @JsonProperty("payload") Object payload) {
        this.commandId = commandId;
        this.payload = payload;
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
