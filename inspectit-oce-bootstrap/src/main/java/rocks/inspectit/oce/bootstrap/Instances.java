package rocks.inspectit.oce.bootstrap;

import rocks.inspectit.oce.bootstrap.context.ContextManager;
import rocks.inspectit.oce.bootstrap.noop.NoopContextManager;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {

    public static ContextManager contextManager = NoopContextManager.INSTANCE;

}
