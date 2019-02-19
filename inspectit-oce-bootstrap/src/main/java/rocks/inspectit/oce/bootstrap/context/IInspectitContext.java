package rocks.inspectit.oce.bootstrap.context;

import java.util.Map;

/**
 * Interface abstraction for the InspectitContext.
 */
public interface IInspectitContext extends AutoCloseable {


    void setData(String key, Object value);

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
