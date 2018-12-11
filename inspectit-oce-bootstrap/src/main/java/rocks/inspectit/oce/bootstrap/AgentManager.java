package rocks.inspectit.oce.bootstrap;

/**
 * Manages the running Agent. This class is responsible for starting and stopping {@link rocks.inspectit.oce.core.AgentImpl}
 *
 * @author Jonas Kunz
 */
public class AgentManager {

    public static IAgent agentInstance = null;

    public static synchronized void startOrReplaceInspectitCore(ClassLoader inspectITClassLoader) {
        if (agentInstance != null) {
            agentInstance.destroy();
            agentInstance = null;
        }
        try {
            Class<?> implClass = Class.forName("rocks.inspectit.oce.core.AgentImpl", true, inspectITClassLoader);
            agentInstance = (IAgent) implClass.newInstance();
            agentInstance.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
