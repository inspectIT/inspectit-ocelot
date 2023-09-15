package rocks.inspectit.ocelot.bootstrap.exposed;

import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;

import java.util.Map;
import java.util.Set;

/**
 * The view on the inspectIT context which is accessible from within actions.
 */
public interface InspectitContext {

    /**
     * Assigns the given value to the given data key.
     * Depending on how the propagation is configured for the given key, the value will be propagated up or down.
     * All changes made through this method before {@link InternalInspectitContext#makeActive()} is called will be visible for
     * all child contexts. If this method is called after {@link InternalInspectitContext#makeActive()}, the changes will only be visible
     * for synchronous child contexts.
     *
     * @param key   the name of the data to assign the value to
     * @param value the value to assign, can be null meaning that the data should be cleared
     */
    void setData(String key, Object value);

    /**
     * Returns the last value assigned for the given data key.
     * The value was either defined via {@link #setData(String, Object)}, down or up propagation.
     *
     * @param key the name of the data to query
     * @return the value assigned to the data or null if no value is assigned
     */
    Object getData(String key);

    /**
     * Returns all current data as an iterable.
     *
     * @return the data iterable
     */
    Iterable<Map.Entry<String, Object>> getData();

    /**
     * This function should be called in the entry- or pre-entry-phase, to allow the created span to use the context
     *
     * Creates a SpanContext locally, which the current InspectitContext can use as a remote-parent-context,
     * as long as no REMOTE_PARENT_SPAN_CONTEXT_KEY was specified earlier by down-propagation
     *
     * @return The trace context of the created SpanContext in the W3C-format
     */
    String createRemoteParentContext();

    /**
     * Generates a map representing the globally down-propagated data stored in this context.
     * The map is designed so that the keys can be used as HTTP header names and the values as corresponding header values.
     * However, the contents of this map can be also used for any other protocol.
     *
     * @return the propagation map
     */
    Map<String, String> getDownPropagationHeaders();

    /**
     * Generates a map representing the globally up-propagated data stored in this context.
     * The map is designed so that the keys can be used as HTTP header names and the values as corresponding header values.
     * However, the contents of this map can be also used for any other protocol.
     *
     * @return the propagation map
     */
    Map<String, String> getUpPropagationHeaders();

    /**
     * Opposite method for {@link #getDownPropagationHeaders()}.
     * This method takes a map from header names to header values and extracts the propagated data from them.
     * The header names which are of interest for the propagation can be queried via {@link #getPropagationHeaderNames()}.
     * The difference to {@link #readUpPropagationHeaders(Map)} is that this method also extracts trace correlation information
     */
    void readDownPropagationHeaders(Map<String, String> headers);

    /**
     * Opposite method for {@link #getUpPropagationHeaders()}.
     * This method takes a map from header names to header values and extracts the propagated data from them.
     * The header names which are of interest for the propagation can be queried via {@link #getPropagationHeaderNames()}.
     */
    void readUpPropagationHeaders(Map<String, String> headers);

    /**
     * @return the names of Http headers which are relevant for the context propagation.
     */
    Set<String> getPropagationHeaderNames();
}
