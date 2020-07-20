package rocks.inspectit.ocelot.sdk.events;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;

@Slf4j
public class EventRegistryService {
    /** Stores registered EventHandlers. */
    private static HashMap<String, OcelotEventPluginHandler> exporters = new HashMap<>();

    public void sendEventsToExporters(Collection<Event> events) {
        if(events == null) {
            return;
        }
        for(HashMap.Entry<String, OcelotEventPluginHandler> entry : exporters.entrySet()) {
            OcelotEventPluginHandler handler = entry.getValue();
            try {
                handler.export(events);
            } catch (Throwable t) {
                String handlerName = entry.getKey();
                log.error("EventExporter Handler " + handlerName + " threw an error while receiving events and will be unregistered.", t);
                unregisterHandler(handlerName);
            }
        }
    }

    public static void registerHandler(String name, OcelotEventPluginHandler handler) {
        log.info("Registering handler: " + name + " to EventRegistryService");
        if(exporters.containsKey(name)) {
            log.error("Failed to register event exporter handler '{}' as a handler with this name is already registered.", name);
            return;
        }
        exporters.put(name, handler);
    }

    public static void unregisterHandler(String name) {
        log.info("Unregistering handler: " + name + " from EventRegistryService");
        exporters.remove(name);
    }
}
