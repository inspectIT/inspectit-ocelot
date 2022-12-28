package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Custom implementation of {@link OpenTelemetry} that wraps {@link OpenTelemetrySdk}.
 * This wrapper class is used to change {@link #openTelemetry} without the exception handling of {@link io.opentelemetry.api.GlobalOpenTelemetry#set(OpenTelemetry)}
 */
@Slf4j
@AllArgsConstructor
public class OpenTelemetryImpl implements OpenTelemetry {

    /**
     * The {@link OpenTelemetrySdk} implementation
     */
    @NotNull
    private OpenTelemetrySdk openTelemetry;

    private final Object lock = new Object();

    @Override
    public TracerProvider getTracerProvider() {
        return openTelemetry.getTracerProvider();
    }

    @Override
    public ContextPropagators getPropagators() {
        return openTelemetry.getPropagators();
    }

    @Override
    public MeterProvider getMeterProvider() {
        return openTelemetry.getMeterProvider();
    }

    /**
     * Gets the {@link SdkMeterProvider} of the currently registered {@link #openTelemetry}}
     *
     * @return
     */
    private SdkMeterProvider getSdkMeterProvider() {
        return openTelemetry.getSdkMeterProvider();
    }

    /**
     * Gets the {@link SdkTracerProvider} of the currently registered {@link #openTelemetry}}
     *
     * @return
     */
    private SdkTracerProvider getSdkTracerProvider() {
        return openTelemetry.getSdkTracerProvider();
    }

    /**
     * Registers the {@link OpenTelemetry}. If an {@link OpenTelemetrySdk} was already registered as {@link #openTelemetry}, flush and close the {@link SdkTracerProvider} and {@link SdkMeterProvider} of the previously registered {@link OpenTelemetrySdk} if required before registering the new {@link OpenTelemetrySdk}
     *
     * @param openTelemetry              The new {@link OpenTelemetrySdk} to register.
     * @param stopPreviousTracerProvider Whether the {@link SdkTracerProvider} of a previously registered {@link OpenTelemetrySdk} shall be closed before registering the new {@link OpenTelemetrySdk}
     * @param stopPreviousMeterProvider  Whether the {@link SdkMeterProvider} of a previously registered {@link OpenTelemetrySdk} shall be closed before registering the new {@link OpenTelemetrySdk}
     */
    public void set(OpenTelemetrySdk openTelemetry, boolean stopPreviousTracerProvider, boolean stopPreviousMeterProvider) {
        synchronized (lock) {
            if (null != openTelemetry) {
                if (stopPreviousTracerProvider) {
                    OpenTelemetryUtils.stopTracerProvider(getSdkTracerProvider());
                }
                if (stopPreviousMeterProvider) {
                    OpenTelemetryUtils.stopMeterProvider(getSdkMeterProvider());
                }
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
        synchronized (lock) {
            CompletableResultCode closeTracerProviderResultCode = OpenTelemetryUtils.stopTracerProvider(getSdkTracerProvider(), true);
            CompletableResultCode closeMeterProviderResultCode = OpenTelemetryUtils.stopMeterProvider(getSdkMeterProvider(), true);
            return CompletableResultCode.ofAll(Arrays.asList(closeMeterProviderResultCode, closeTracerProviderResultCode));
        }
    }

    /**
     * Flushes the {@link SdkMeterProvider} and {@link SdkTracerProvider} and waits for it to complete.
     */
    public void flush() {
        CompletableResultCode flushTracerProvider = null != getSdkTracerProvider() ? getSdkTracerProvider().forceFlush() : CompletableResultCode.ofSuccess();
        CompletableResultCode flushMeterProvider = null != getMeterProvider() ? getSdkMeterProvider().forceFlush() : CompletableResultCode.ofSuccess();
        CompletableResultCode resultCode = CompletableResultCode.ofAll(Arrays.asList(flushTracerProvider, flushMeterProvider));
        if (!resultCode.isDone()) {
            CountDownLatch latch = new CountDownLatch(1);
            resultCode.whenComplete(latch::countDown);
            try {
                latch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for the OpenTelemetryImpl to flush", e);
            }
        }
    }

    /**
     * Registers {@link OpenTelemetryImpl this} to {@link GlobalOpenTelemetry#set(OpenTelemetry)}.
     */
    public void registerGlobal() {
        GlobalOpenTelemetry.set(this);
    }

}
