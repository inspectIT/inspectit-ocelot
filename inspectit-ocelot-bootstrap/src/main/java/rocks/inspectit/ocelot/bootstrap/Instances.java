package rocks.inspectit.ocelot.bootstrap;

import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IObjectAttachments;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopObjectAttachments;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {

    public static IContextManager contextManager = NoopContextManager.INSTANCE;

    public static IHookManager hookManager = NoopHookManager.INSTANCE;

    public static IObjectAttachments attachments = NoopObjectAttachments.INSTANCE;
}
