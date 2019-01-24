package rocks.inspectit.oce.bootstrap;

/**
 * Accessor for implementations of the interfaces.
 * The values are replaced by the actual implementations when an inspectit-core is started.
 */
public class Instances {
    public static ContextManager contextManager = ContextManager.NOOP;
}
