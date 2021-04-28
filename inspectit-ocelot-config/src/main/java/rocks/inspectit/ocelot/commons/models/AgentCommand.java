package rocks.inspectit.ocelot.commons.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Represents a command to be executed by an inspecIT agent.
 */
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AgentCommand {

    public static AgentCommand emptyCommand;

    public static AgentCommand getEmptyCommand() {
        if (emptyCommand == null) {
            emptyCommand = new AgentCommand(null, null, null, null);
        }
        return emptyCommand;
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
