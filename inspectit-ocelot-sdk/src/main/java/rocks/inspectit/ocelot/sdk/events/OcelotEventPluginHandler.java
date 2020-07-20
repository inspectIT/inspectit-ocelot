package rocks.inspectit.ocelot.sdk.events;

import java.util.Collection;

/**
 * Custom Event Handler which attempt to receive Ocelot Events must implement this class.
 */
public abstract class OcelotEventPluginHandler {

    /**
     * Export function will receive a collection of event-objects periodically.
     */
    public abstract void export(Collection<Event> events);
}
