package rocks.inspectit.ocelot.commons.models.status;

import ch.qos.logback.classic.Level;

/**
 * Represents the status of an individual agent.
 */
public enum AgentStatus {

    OK, WARNING, ERROR;

    /**
     * Decides whether this status is more severe or equal to the passed one.
     * {@code null} is always considered less severe.
     *
     * @param other The status to compare with
     *
     * @return {@code true} if both status are equal or this is more severe than other; {@code false} otherwise.
     */
    public boolean isMoreSevereOrEqualTo(AgentStatus other) {
        return other != null ? compareTo(other) >= 0 : true;
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

    /**
     * Determines the agent status based on the level of a log event that occurred (e.g., WARN level corresponds with WARNING).
     *
     * @param logLevel The log level that occurred
     *
     * @return The agent status that corresponds to the log level.
     */
    public static AgentStatus fromLogLevel(Level logLevel) {
        if (logLevel.isGreaterOrEqual(Level.ERROR)) {
            return ERROR;
        } else if (logLevel.isGreaterOrEqual(Level.WARN)) {
            return WARNING;
        } else {
            return OK;
        }
    }

}
