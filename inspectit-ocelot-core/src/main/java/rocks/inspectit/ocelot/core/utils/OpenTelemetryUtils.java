package rocks.inspectit.ocelot.core.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableMeasurement;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.internal.descriptor.InstrumentDescriptor;
import io.opentelemetry.sdk.metrics.internal.state.SdkObservableMeasurement;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.opentelemetry.CustomTracer;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to work with OpenTelemetry, i.e., {@link GlobalOpenTelemetry}, {@link SdkTracerProvider}, {@link SdkMeterProvider}
 */
@Slf4j
public class OpenTelemetryUtils {

    // TODO: move to another class (e.g., MeasurementUtils), single-responsibility!

    /**
     * The {@link InstrumentDescriptor #instrumentDescriptor} member of {@link SdkObservableMeasurement}
     */
    private static final Field SDKOBSERVABLEMEASUREMENT_INSTRUMENTDESCRIPTOR;

    private static final Field ABSTRACTINSTRUMENT_INSTRUMENTDESCRIPTOR;

    private static final Class ABSTRACTINSTRUMENT_CLASS;

    static {
        try {
            SDKOBSERVABLEMEASUREMENT_INSTRUMENTDESCRIPTOR = ReflectionUtils.getFieldAndMakeAccessible(SdkObservableMeasurement.class, "instrumentDescriptor");
            ABSTRACTINSTRUMENT_CLASS = Class.forName("io.opentelemetry.sdk.metrics.AbstractInstrument");
            ABSTRACTINSTRUMENT_INSTRUMENTDESCRIPTOR = ReflectionUtils.getFieldAndMakeAccessible(ABSTRACTINSTRUMENT_CLASS, "descriptor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public final static String DEFAULT_INSTRUMENTATION_SCOPE_INFO = "rocks.inspectit.ocelot";

    public final static String DEFAULT_INSTRUMENTATION_SCOPE_VERSION = "0.0.1";

    /**
     * Gets the current {@link Tracer} registered at {@link GlobalOpenTelemetry#getTracer(String, String)} under the instrumentationScopeName 'rocks.inspectit.ocelot'
     *
     * @return
     */
    public static Tracer getTracer() {
        return getGlobalOpenTelemetry().getTracer(DEFAULT_INSTRUMENTATION_SCOPE_INFO, DEFAULT_INSTRUMENTATION_SCOPE_VERSION);
    }

    /**
     * Gets the current {@link io.opentelemetry.api.metrics.Meter} registered at {@link GlobalOpenTelemetry#getMeter(String)} under the instrumentationScopeName {@link #DEFAULT_INSTRUMENTATION_SCOPE_INFO}
     *
     * @return
     */
    public static Meter getMeter() {
        return getGlobalOpenTelemetry().getMeter(DEFAULT_INSTRUMENTATION_SCOPE_INFO);
    }

    /**
     * Gets a custom {@link Tracer tracer} with custom {@link Sampler sampler}
     *
     * @param customSampler
     *
     * @return
     */
    public static Tracer getTracer(Sampler customSampler) {
        return CustomTracer.builder().sampler(customSampler).build();
    }

    /**
     * Gets the {@link InstrumentDescriptor} for a given {@link SdkObservableMeasurement}
     *
     * @param observableMeasurement
     *
     * @return
     */
    private static InstrumentDescriptor getInstrumentDescriptor(ObservableMeasurement observableMeasurement) {
        if (!(observableMeasurement instanceof SdkObservableMeasurement)) {
            throw new IllegalArgumentException(observableMeasurement.getClass() + " is not of type " + SdkObservableMeasurement.class.getName());
        }
        try {
            return (InstrumentDescriptor) SDKOBSERVABLEMEASUREMENT_INSTRUMENTDESCRIPTOR.get(observableMeasurement);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not extract InstrumentDescriptor", e);
        }
    }

    /**
     * Gets the {@link InstrumentDescriptor} for a given {@link io.opentelemetry.sdk.metrics.AbstractInstrument} or {@link ObservableMeasurement}
     *
     * @param instrument the instrument
     *
     * @return
     *
     * @throws IllegalAccessException
     */
    public static InstrumentDescriptor getInstrumentDescriptor(Object instrument) {
        if (ABSTRACTINSTRUMENT_CLASS.isInstance(instrument)) {
            try {
                return (InstrumentDescriptor) ABSTRACTINSTRUMENT_INSTRUMENTDESCRIPTOR.get(instrument);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not extract InstrumentDescriptor", e);
            }
        } else if (instrument instanceof ObservableMeasurement) {
            return getInstrumentDescriptor((ObservableMeasurement) instrument);
        } else {
            throw new RuntimeException(String.format("Cold not extract %s for class %s", InstrumentDescriptor.class.getName(), instrument.getClass()
                    .getName()));
        }

    }

}