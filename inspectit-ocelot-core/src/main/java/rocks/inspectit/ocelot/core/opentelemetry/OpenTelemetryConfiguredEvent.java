package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * An event when a new {@link OpenTelemetry} has been configured and registered as the {@link io.opentelemetry.api.GlobalOpenTelemetry}
 */
public class OpenTelemetryConfiguredEvent extends ApplicationEvent {

    /**
     * The newly configured {@link OpenTelemetry}
     */
    @Getter
    private final OpenTelemetry openTelemetry;

    /**
     * Whether {@link #openTelemetry} was successfully configured
     */
    @Getter
    private final boolean success;

    public OpenTelemetryConfiguredEvent(Object source, boolean success, OpenTelemetry openTelemetry) {
        super(source);
        this.openTelemetry = openTelemetry;
        this.success = success;
    }
}
