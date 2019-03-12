package rocks.inspectit.oce.bootstrap.instrumentation;

/**
 * Abstraction for a simple map which allows to attach arbitrary objects to other objects based on a string key.
 * The attachment is performed weakly, meaning that if an object gets garbage collected, all its attachments are removed automatically.
 * <p>
 * In addition this data structure is thread safe.
 */
public interface IObjectAttachments {

    /**
     * Attaches the given value to the given target object under the given key name.
     * Can also be used to clear an entry by setting the value to null.
     *
     * @param target the object to which this value shall be attached
     * @param key    the name under which the value shall be attached
     * @param value  the value to attach or null to remove the value.
     */
    void attach(Object target, String key, Object value);

    /**
     * Reads an attachment given the target object and the key under which the value was attached.
     *
     * @param target The object on which the target value was attached
     * @param key    the name under which the target value was attached
     * @return the value attached under the given name to the given target object, null if nothing was attached under this name
     */
    Object getAttachment(Object target, String key);
}
