package rocks.inspectit.oce.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Manages the running Agent. This class is responsible for starting and stopping {@link rocks.inspectit.oce.core.AgentImpl}
 *
 * @author Jonas Kunz
 */
public class AgentManager {

    public static IAgent agentInstance = null;

    /**
     * If an Agent is already running, invoking this method first stops it.
     * Afterwards it tries to start a new Agent from the given Classpath.
     * @param inspectITClassLoader the classloader of inspectit-core
     * @param agentCmdArgs the command line arguments to pass to the Agent
     * @param  instrumentation the {@link Instrumentation} to pass to the Agent
     */
    public static synchronized void startOrReplaceInspectitCore(ClassLoader inspectITClassLoader, String agentCmdArgs, Instrumentation instrumentation) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(inspectITClassLoader);
        try {
            if (agentInstance != null) {
                agentInstance.destroy();
                agentInstance = null;
            }
            try {
                Class<?> implClass = Class.forName("rocks.inspectit.oce.core.AgentImpl", true, inspectITClassLoader);
                agentInstance = (IAgent) implClass.newInstance();
                agentInstance.start(agentCmdArgs, instrumentation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

    }
}
