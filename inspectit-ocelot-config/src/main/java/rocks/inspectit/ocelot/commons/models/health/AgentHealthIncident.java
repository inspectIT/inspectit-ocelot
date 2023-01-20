package rocks.inspectit.ocelot.commons.models.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentHealthIncident implements Comparable{

    private String time;
    private AgentHealth health;
    private String source;
    private String message;
    private boolean changedHealth;

    public static AgentHealthIncident getNoneIncident() {
        return new AgentHealthIncident(LocalDateTime.now().toString(), AgentHealth.OK, "", "", false);
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof AgentHealthIncident)) {
            return 1;
        }
        AgentHealthIncident healthIncident = (AgentHealthIncident) o;
        return this.getHealth().compareTo(healthIncident.getHealth());
    }
}
