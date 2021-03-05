package rocks.inspectit.ocelot.agentcommand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
@AllArgsConstructor
public class AgentCommand {

    private AgentCommandType commandType;

    private List<Object> parameters;
}
