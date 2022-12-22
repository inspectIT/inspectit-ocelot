package rocks.inspectit.ocelot.core.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to work with OpenTelemetry, i.e., {@link GlobalOpenTelemetry}, {@link SdkTracerProvider}, {@link SdkMeterProvider}
 */
@Slf4j
public class OpenTelemetryUtils {

    /**
     * {@link SdkMeterProvider#close() closes} the given {@link SdkMeterProvider} and blocks waiting for it to complete.
     *
     * @param meterProvider The {@link SdkMeterProvider} to stop.
     *
     * @return The {@link CompletableResultCode}
     */
    public static CompletableResultCode stopMeterProvider(SdkMeterProvider meterProvider) {
        return stopMeterProvider(meterProvider, false);
    }

    /**
     * {@link SdkMeterProvider#close() Closes} the given {@link SdkMeterProvider} and optionally {@link SdkMeterProvider#forceFlush() force flushes}, and blocks waiting for it to complete.
     *
     * @param meterProvider
     * @param forceFlush    Whether to call {@link SdkMeterProvider#forceFlush()}
     *
     * @return The {@link CompletableResultCode}
     */
    public static CompletableResultCode stopMeterProvider(SdkMeterProvider meterProvider, boolean forceFlush) {
        // force flush if applicable
        if (forceFlush) {
            // wait until force flush has succeeded
            CompletableResultCode flushResult = meterProvider.forceFlush();
            if (!flushResult.isDone()) {
                CountDownLatch latch = new CountDownLatch(1);
                flushResult.whenComplete(() -> latch.countDown());
                try {
                    latch.await(10, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    log.error("failed to force flush SdkMeterProvider", t);
                    t.printStackTrace();
                    return CompletableResultCode.ofFailure();
                }
            }
        }

        // close the SdkMeterProvider. This calls shutDown internally and waits blocking for it.
        meterProvider.close();
        return CompletableResultCode.ofSuccess();
    }

    /**
     * {@link SdkTracerProvider#close() Closes} the given {@link SdkTracerProvider} and blocks waiting for it to complete.
     *
     * @param tracerProvider The {@link SdkTracerProvider} to shut down
     */
    public static CompletableResultCode stopTracerProvider(SdkTracerProvider tracerProvider) {
        return stopTracerProvider(tracerProvider, false);
    }

    /**
     * {@link SdkTracerProvider#close() stops} the given {@link SdkTracerProvider} and optionally {@link SdkTracerProvider#forceFlush() flushes} it before {@link SdkTracerProvider#close() closing}, and blocks waiting for it to complete.
     *
     * @param tracerProvider The {@link SdkTracerProvider} to stop
     * @param forceFlush     Whether to call {@link SdkTracerProvider#forceFlush()} before closing it.
     */
    public static CompletableResultCode stopTracerProvider(SdkTracerProvider tracerProvider, boolean forceFlush) {
        if (null != tracerProvider) {
            long start = System.nanoTime();
            if (forceFlush) {
                CompletableResultCode flushResultCode = tracerProvider.forceFlush();
                if (!flushResultCode.isDone()) {
                    long startFlush = System.nanoTime();
                    CountDownLatch latch = new CountDownLatch(1);
                    flushResultCode.whenComplete(() -> latch.countDown());
                    try {
                        latch.await(15, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        log.error("failed to forceFlush tracerProvider", e);
                        e.printStackTrace();
                        return CompletableResultCode.ofFailure();
                    }
                }
            }
            tracerProvider.close();
        }
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Gets the {@link OpenTelemetry} registered at {@link GlobalOpenTelemetry#globalOpenTelemetry} without calling {@link GlobalOpenTelemetry#get()} to avoid that it is assigned to {@link OpenTelemetry#noop()} on the first call.
     *
     * @return The {@link OpenTelemetry} registered at {@link GlobalOpenTelemetry#globalOpenTelemetry}
     */
    public static OpenTelemetry getGlobalOpenTelemetry() {
        OpenTelemetry openTelemetry = null;
        try {
            Field field = ReflectionUtils.getFieldAndMakeAccessible(GlobalOpenTelemetry.class, "globalOpenTelemetry");
            openTelemetry = (OpenTelemetry) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return openTelemetry;
    }

    /**
     * {@link Instances#openTelemetryController#flush() flushes} all pending spans and metrics waits for it to complete.
     */
    public static void flush() {
        Instances.openTelemetryController.flush();
    }

    /**
     * Gets the current {@link Tracer} registered at {@link GlobalOpenTelemetry#getTracer(String, String)} under the instrumentationScopeName 'rocks.inspectit.ocelot'
     *
     * @return
     */
    public static Tracer getTracer() {
        return getGlobalOpenTelemetry().getTracer("rocks.inspectit.ocelot", "0.0.1");
    }

}