package rocks.inspectit.oce.bootstrap;

import rocks.inspectit.oce.bootstrap.context.IContextManager;
import rocks.inspectit.oce.bootstrap.noop.NoopContextManager;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {

    public static IContextManager contextManager = NoopContextManager.INSTANCE;

}
