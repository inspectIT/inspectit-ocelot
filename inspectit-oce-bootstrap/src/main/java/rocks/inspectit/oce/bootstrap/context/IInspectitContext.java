package rocks.inspectit.oce.bootstrap.context;

import java.util.Map;

/**
 * Interface abstraction for the InspectitContext.
 */
public interface IInspectitContext extends AutoCloseable {


    /**
     * Assigns the given value to the given data key.
     * Depending on how the propagation is configured for the given key, the value will be propagated up or down.
     * All changes made through this method before {@link #makeActive()} is called will be visible for
     * all child contexts. If this method is called after {@link #makeActive()}, the changes will only be visible
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
     * Returns all current data as an iteratable.
     *
     * @return the data iteratable
     */
    Iterable<Map.Entry<String, Object>> getData();

    /**
     * Makes this context the active one.
     * This means all new contexts created from this point will use this context as a parent.
     * Async context will see down-propagated data in the state it was when this method was calle.
     */
    void makeActive();

    /**
     * Closes this context, performing up-propagation if required.
     */
    @Override
    void close();
}
