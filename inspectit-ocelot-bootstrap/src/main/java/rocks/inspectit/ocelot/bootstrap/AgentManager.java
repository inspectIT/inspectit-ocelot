package rocks.inspectit.ocelot.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Manages the running agent. This class is responsible for starting and stopping the active {@link IAgent} implementation.
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
     * If an agent is already running, invoking this method first stops it.
     * Afterward it tries to start a new agent from the given classpath.
     *
     * @param inspectITClassLoader the classloader of inspectit-ocelot-core
     * @param agentCmdArgs         the command line arguments to pass to the agent
     * @param instrumentation      the {@link Instrumentation} to pass to the agent
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
                agentInstance = (IAgent) implClass.getDeclaredConstructor().newInstance();
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
     * @return true if an agent has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the active {@link IAgent} implementation instance
     */
    public static IAgent getAgent() {
        return agentInstance;
    }

    /**
     * @return the current agent version
     */
    public static String getAgentVersion() {
        if (agentInstance == null) {
            return "UNKNOWN";
        } else {
            return agentInstance.getVersion();
        }
    }

    /**
     * @return the current OTel version the agent was build with
     */
    public static String getOpenTelemetryVersion() {
        if (agentInstance == null) {
            return "UNKNOWN";
        } else {
            return agentInstance.getOpenTelemetryVersion();
        }
    }
}
