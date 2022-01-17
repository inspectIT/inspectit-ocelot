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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
                // stop previous SdkTracerProvider
                OpenTelemetryUtils.stopTracerProvider(this.openTelemetry.getSdkTracerProvider());
            }
            this.openTelemetry = openTelemetry;
        }
    }

    /**
     * Shuts down the currently registered {@link #openTelemetry#getSdkTracerProvider() SdkTracerProvider} and blocks waiting for it to complete.
     *
     * @return The {@link CompletableResultCode}
     */
    public synchronized CompletableResultCode close() {
        CompletableResultCode result;
        synchronized (lock) {
            result = OpenTelemetryUtils.stopTracerProvider(openTelemetry.getSdkTracerProvider(), true);
        }
        return result;
    }

    /**
     * {@link io.opentelemetry.sdk.trace.SdkTracerProvider#forceFlush() flushes} the {@link #openTelemetry} and waits for it to complete.
     */
    public void flush() {
        CompletableResultCode resultCode = openTelemetry.getSdkTracerProvider().forceFlush();
        if (!resultCode.isDone()) {
            CountDownLatch latch = new CountDownLatch(1);
            resultCode.whenComplete(() -> latch.countDown());
            try {
                latch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
