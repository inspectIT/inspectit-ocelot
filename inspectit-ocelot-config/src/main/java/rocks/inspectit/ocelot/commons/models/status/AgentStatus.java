package rocks.inspectit.ocelot.commons.models.status;

import lombok.AllArgsConstructor;

/**
 * Represents the status of an individual agent.
 */
@AllArgsConstructor
public enum AgentStatus {

    OK(10), WARNING(20), ERROR(30);

    private final int severity;

    /**
     * Decides whether this status is more severe or equal to the passed one.
     * {@code null} is always considered less severe.
     *
     * @param other The status to compare with
     *
     * @return {@code true} if both status are equal or this is more severe than other; {@code false} otherwise.
     */
    public boolean isMoreSevereOrEqualTo(AgentStatus other) {
        return other != null ? other.severity >= severity : true;
    }

    /**
     * Compares multiple status and returns the one that is most severe.
     *
     * @param status The array of status to compare (may contain {@code null})
     *
     * @return That status of the passed ones that is more severe. {@code null} if none is passed.
     */
    public static AgentStatus mostSevere(AgentStatus... status) {
        AgentStatus max = null;

        for (AgentStatus curr : status) {
            if (curr != null && curr.isMoreSevereOrEqualTo(max)) {
                max = curr;
            }
        }

        return max;
    }

}
