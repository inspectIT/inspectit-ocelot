package rocks.inspectit.ocelot.commons.models.health;

import ch.qos.logback.classic.Level;

/**
 * Represents the health status of an individual agent.
 */
public enum AgentHealth {

    OK, WARNING, ERROR;

    /**
     * Decides whether this health status is more severe or equal to the passed one.
     * {@code null} is always considered less severe.
     *
     * @param other The health to compare with
     *
     * @return {@code true} if both health status are equal or this is more severe than other; {@code false} otherwise.
     */
    public boolean isMoreSevereOrEqualTo(AgentHealth other) {
        return other != null ? compareTo(other) >= 0 : true;
    }

    /**
     * Compares multiple health status and returns the one that is most severe.
     *
     * @param status The array of health status to compare (may contain {@code null})
     *
     * @return That health of the passed ones that is more severe. {@code null} if none is passed.
     */
    public static AgentHealth mostSevere(AgentHealth... status) {
        AgentHealth max = null;

        for (AgentHealth curr : status) {
            if (curr != null && curr.isMoreSevereOrEqualTo(max)) {
                max = curr;
            }
        }

        return max;
    }

    /**
     * Determines the agent health based on the level of a log event that occurred (e.g., WARN level corresponds with WARNING).
     *
     * @param logLevel The log level that occurred
     *
     * @return The agent health that corresponds to the log level.
     */
    public static AgentHealth fromLogLevel(Level logLevel) {
        if (logLevel.isGreaterOrEqual(Level.ERROR)) {
            return ERROR;
        } else if (logLevel.isGreaterOrEqual(Level.WARN)) {
            return WARNING;
        } else {
            return OK;
        }
    }

}
