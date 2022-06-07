package rocks.inspectit.ocelot.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Controller interface for the Agent. Its implementation is {@link rocks.inspectit.ocelot.core.AgentImpl}.
 * The implementation must provide a default cosntructor without side effects!
 * The actual initialization should happen in {@link #start(String, Instrumentation)}, which is called by {@link AgentManager}
 */
public interface IAgent {

    /**
     * Initialized and starts the agent.
     *
     * @param agentCmdArgs    the command line arguments passed to the Agent
     * @param instrumentation the {@link Instrumentation} instance passed to the agent
     */
    void start(String agentCmdArgs, Instrumentation instrumentation);

    /**
     * Shuts down and destroys the agent.
     */
    void destroy();

    /**
     * Returns the agent version.
     *
     * @return the version of the currently used agent
     */
    String getVersion();

    /**
     * Returns the version of Open Telemetry the agent was build with.
     *
     * @return the version of Open Telemetry currently used in the agent.
     */
    String getOpenTelemetryVersion();

    /**
     * Returns the date the agent has been built.
     *
     * @return string representing when the date the agent has been built
     */
    String getBuildDate();
}
