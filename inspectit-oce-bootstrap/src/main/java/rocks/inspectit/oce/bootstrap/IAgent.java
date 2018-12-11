package rocks.inspectit.oce.bootstrap;

/**
 * Controller itnerface for the Agent. Its implementation is {@link rocks.inspectit.oce.core.AgentImpl}.
 * The implementation must provide a default cosntructor without side effects!
 * The actual initialization should happen in {@link #start()}, which is called by {@link AgentManager}
 */
public interface IAgent {

    /**
     * Initialized and starts the agent.
     */
    void start();

    /**
     * Shuts down and destroys the agent.
     */
    void destroy();

}
