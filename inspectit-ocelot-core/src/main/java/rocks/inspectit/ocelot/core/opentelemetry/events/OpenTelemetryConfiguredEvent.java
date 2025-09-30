package rocks.inspectit.ocelot.core.opentelemetry.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class OpenTelemetryConfiguredEvent extends ApplicationEvent {

    @Getter
    private final boolean success;

    public OpenTelemetryConfiguredEvent(Object source, boolean success) {
        super(source);
        this.success = success;
    }
}
