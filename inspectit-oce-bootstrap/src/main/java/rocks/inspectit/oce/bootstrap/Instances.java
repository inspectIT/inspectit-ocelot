package rocks.inspectit.oce.bootstrap;

import rocks.inspectit.oce.bootstrap.context.IContextManager;
import rocks.inspectit.oce.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.oce.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.oce.bootstrap.instrumentation.IObjectAttachments;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopObjectAttachments;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {

    public static IContextManager contextManager = NoopContextManager.INSTANCE;

    public static IHookManager hookManager = NoopHookManager.INSTANCE;

    public static IObjectAttachments attachments = NoopObjectAttachments.INSTANCE;

}
