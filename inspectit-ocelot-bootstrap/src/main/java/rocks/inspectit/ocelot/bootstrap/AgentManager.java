package rocks.inspectit.ocelot.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Manages the running Agent. This class is responsible for starting and stopping the active {@link IAgent} implementation.
 *
 * @author Jonas Kunz
 */
public class AgentManager {

    /**
     * The current agent instance.
     */
    private static IAgent agentInstance = null;

    /**
     * Specifies whether an agent is initialized or not.
     */
    private static volatile boolean initialized = false;

    /**
     * If an Agent is already running, invoking this method first stops it.
     * Afterwards it tries to start a new Agent from the given Classpath.
     *
     * @param inspectITClassLoader the classloader of inspectit-core
     * @param agentCmdArgs         the command line arguments to pass to the Agent
     * @param instrumentation      the {@link Instrumentation} to pass to the Agent
     */
    public static synchronized void startOrReplaceInspectitCore(ClassLoader inspectITClassLoader, String agentCmdArgs, Instrumentation instrumentation) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(inspectITClassLoader);
        try {
            if (agentInstance != null) {
                agentInstance.destroy();
                agentInstance = null;
                initialized = false;
            }
            try {
                Class<?> implClass = Class.forName("rocks.inspectit.ocelot.core.AgentImpl", true, inspectITClassLoader);
                agentInstance = (IAgent) implClass.newInstance();
                agentInstance.start(agentCmdArgs, instrumentation);
                initialized = true;
            } catch (Exception e) {
                System.err.println("Agent could not been started.");
                e.printStackTrace();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * Returns whether an agent is initialized or not.
     *
     * @return Returns true if an agent has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the agent which is currently active.
     *
     * @return the active {@link IAgent} implementation instance
     */
    public static IAgent getAgent() {
        return agentInstance;
    }

    /**
     * Returns the version of the current agent.
     *
     * @return the current agent version
     */
    public static String getAgentVersion() {
        if (agentInstance == null) {
            return "UNKNOWN";
        } else {
            return agentInstance.getVersion();
        }
    }
}
