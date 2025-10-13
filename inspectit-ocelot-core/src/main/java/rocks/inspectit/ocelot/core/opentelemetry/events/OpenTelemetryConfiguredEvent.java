package rocks.inspectit.ocelot.core.opentelemetry.events;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class OpenTelemetryConfiguredEvent extends ApplicationEvent {

    /**
     * Whether OpenTelemetry was configured successfully
     */
    @Getter
    private final boolean success;

    /**
     * Whether we have to update the configured metrics components, like instruments
     */
    @Getter
    private final boolean updateMetrics;

    public OpenTelemetryConfiguredEvent(Object source, boolean success, boolean updateMetrics) {
        super(source);
        this.success = success;
        this.updateMetrics = updateMetrics;
    }
}
