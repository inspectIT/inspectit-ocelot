package rocks.inspectit.ocelot.agentcommand;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a command to be executed by an inspecIT agent.
 */
@Data
@Getter
public class AgentCommand {

    public static AgentCommand emptyCommand;

    public static AgentCommand getEmptyCommand() {
        if (emptyCommand == null) {
            emptyCommand = new AgentCommand(null, null, null, null);
        }
        return emptyCommand;
    }

    public AgentCommand(@JsonProperty("commandType") AgentCommandType commandType, @JsonProperty("agentId") String agentId, @JsonProperty("commandId") UUID commandId, @JsonProperty("parameters") List<Object> parameters) {
        this.commandType = commandType;
        this.parameters = parameters;
        this.agentId = agentId;
        this.commandId = commandId;
    }

    /**
     * The type of command of this instance.
     */
    private AgentCommandType commandType;

    /**
     * The id of the agent this command is meant for.
     */
    private String agentId;

    /**
     * The id of this command.
     */
    private UUID commandId;

    /**
     * Additional parameters for this command.
     */
    private List<Object> parameters;
}
