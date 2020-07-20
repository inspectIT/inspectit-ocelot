package rocks.inspectit.ocelot.sdk.events;

import lombok.Data;

import java.util.Map;

/**
 * Event object which will be sent towards exporters which have been registered at {@link core/exporter/EventExporterService}
 */
@Data
public class Event {
    String name;

    Long timestamp;

    Map<String, Object> attributes;
}
