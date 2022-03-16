package rocks.inspectit.ocelot.commons.models.status;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Represents the status of an individual agent.
 */
@AllArgsConstructor
public enum AgentStatus {

    OK(10), WARNING(20), ERROR(30);

    private final int index;

    public boolean isMoreSevereOrEqualTo(@NonNull AgentStatus other) {
        return other.index >= index;
    }

}
