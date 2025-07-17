package rocks.inspectit.ocelot.bootstrap.exposed;

/**
 * The API to access agent information within actions.
 */
public interface InspectitAgentInfo {

    /**
     * @return the current agent version
     */
    String currentVersion();

    /**
     * @param requiredVersion the required version
     *
     * @return true, if the agent is at least the required version
     */
    boolean isAtLeastVersion(String requiredVersion);
}
