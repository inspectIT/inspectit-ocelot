package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

/**
 * Custom implementation of {@link OpenTelemetry} that wraps {@link OpenTelemetrySdk}.
 * This wrapper class is used to change {@link #openTelemetry} without the exception handling of {@link io.opentelemetry.api.GlobalOpenTelemetry#set(OpenTelemetry)}
 */
@Slf4j
@Builder
public class OpenTelemetryImpl implements OpenTelemetry {

    /**
     * The {@link OpenTelemetrySdk} implementation
     */
    private OpenTelemetrySdk openTelemetry;

    @Builder.Default
    private Object lock = new Object();

    /**
     * Gets the currently registered {@link OpenTelemetry} or {@link OpenTelemetry#noop()} if nothing has been registered.
     *
     * @return
     */
    public OpenTelemetry get() {
        return null == openTelemetry ? OpenTelemetry.noop() : openTelemetry;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return get().getTracerProvider();
    }

    @Override
    public ContextPropagators getPropagators() {
        return get().getPropagators();
    }

    /**
     * Registers the {@link OpenTelemetrySdk}. If an {@link OpenTelemetrySdk} was already registered as {@link #openTelemetry}, flush and close it before registering the new {@link OpenTelemetrySdk}.
     *
     * @param openTelemetry
     */
    public void set(OpenTelemetrySdk openTelemetry) {
        synchronized (lock) {
            if (null != openTelemetry) {
                log.info("Set new OpenTelemetry. Shut down previous ({})", openTelemetry.getClass());
                // stop previous SdkTracerProvider
                OpenTelemetryUtils.stopTracerProvider(this.openTelemetry.getSdkTracerProvider());
            }
            this.openTelemetry = openTelemetry;
        }
    }

    /**
     * Shuts down the currently registered {@link #openTelemetry#getSdkTracerProvider() SdkTracerProvider}
     *
     * @return The {@link CompletableResultCode}
     */
    public synchronized CompletableResultCode shutdown() {
        CompletableResultCode result;
        synchronized (lock) {
            result = OpenTelemetryUtils.stopTracerProvider(openTelemetry.getSdkTracerProvider(), true);
        }
        return result;
    }

    /**
     * Registers {@link OpenTelemetryImpl this} to {@link GlobalOpenTelemetry#set(OpenTelemetry)}
     *
     * @return
     */
    public OpenTelemetryImpl registerGlobal() {
        GlobalOpenTelemetry.set(this);
        return this;
    }

}
