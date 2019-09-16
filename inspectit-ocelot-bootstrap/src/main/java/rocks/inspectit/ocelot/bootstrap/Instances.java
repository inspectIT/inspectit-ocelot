package rocks.inspectit.ocelot.bootstrap;

import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.ocelot.bootstrap.correlation.LogTraceCorrelator;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopObjectAttachments;

import java.net.URL;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {

    /**
     * Contains the URL pointing to the jar file containing all inspectit-bootstrap classes.
     * The AgentMain class is responsible for setting the value correctly.
     * This is required as Javassist needs access to the bytecode of the bootstrap classes for compiling actions.
     */
    public static URL BOOTSTRAP_JAR_URL;

    /**
     * Contains the URL pointing to the agent-jar file containing with which the target application was started.
     * The AgentMain class is responsible for setting the value correctly.
     */
    public static URL AGENT_JAR_URL;

    public static IContextManager contextManager = NoopContextManager.INSTANCE;

    public static IHookManager hookManager = NoopHookManager.INSTANCE;

    public static ObjectAttachments attachments = NoopObjectAttachments.INSTANCE;

    public static LogTraceCorrelator logTraceCorrelator = NoopLogTraceCorrelator.INSTANCE;
}
