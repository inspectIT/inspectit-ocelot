package rocks.inspectit.ocelot.commons.models.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentHealthState {

    private AgentHealth health;
    private String source;
    private String message;
    private List<AgentHealthIncident> history;

    public static AgentHealthState defaultState() {
        return new AgentHealthState(AgentHealth.OK, "", "", Collections.emptyList());
    }
}
