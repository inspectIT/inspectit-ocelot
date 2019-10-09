package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

/**
 * Interface for all adapters for accessing the MDC of a given logging library.
 */
public interface MDCAdapter {

    /**
     * A function to undo changes made to the MDC.
     */
    interface Undo {
        void undoChange();
    }

    /**
     * Writes a given entry to the MDC.
     * If the value is null, the entry is removed from the MDC.
     *
     * @param key   the MDC key to use
     * @param value the value to place in the MDC, null if the value for the key should be erased
     * @return a {@link Undo} which reverts the change performed by this method call.
     */
    Undo set(String key, String value);
}
