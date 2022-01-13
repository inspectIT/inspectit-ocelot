package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Custom implementation of {@link MeterProvider} that wraps {@link SdkMeterProvider}.
 * This wrapper is used to change {@link #meterProvider} without resetting {@link io.opentelemetry.api.metrics.GlobalMeterProvider#set(MeterProvider)}.
 * For use, create the {@link MeterProviderImpl} and set it once to {@link io.opentelemetry.api.metrics.GlobalMeterProvider#set(MeterProvider)}. If the {@link SdkMeterProvider} changes, simply call {@link #set(SdkMeterProvider)}. If the {@link #meterProvider} was previously set, it will be flushed and closed and blocks waiting for it.
 */
@Slf4j
@Builder
public class MeterProviderImpl implements MeterProvider {

    /**
     * The {@link SdkMeterProvider} implementation
     */
    private SdkMeterProvider meterProvider;

    @Builder.Default
    private Object lock = new Object();

    /**
     * Registers the {@link SdkMeterProvider}.
     * If an instance of {@link SdkMeterProvider} was already registered, it is {@link SdkMeterProvider#forceFlush() flushed} and {@link SdkMeterProvider#shutdown() shutdown} and blocks waiting for it to complete before the new {@link SdkMeterProvider} is set to {@link #meterProvider}
     *
     * @param meterProvider
     */
    public void set(SdkMeterProvider meterProvider) {
        synchronized (lock) {
            // shut down previous meterProvider if set
            if (null != this.meterProvider && this.meterProvider != meterProvider) {
                log.info("Set new SdkMeterProvider. Shut down previous ({})", meterProvider);
                OpenTelemetryUtils.stopMeterProvider(this.meterProvider, true);
            }
            // set SdkMeterProvider
            this.meterProvider = meterProvider;
        }
    }

    /**
     * Gets the currently registered {@link SdkMeterProvider}
     *
     * @return The currently registered {@link SdkMeterProvider}
     */
    public SdkMeterProvider get() {
        return meterProvider;
    }

    /**
     * Shuts down the currently registered {@link #meterProvider} and blocks waiting for it to complete.
     *
     * @return
     */
    public CompletableResultCode close() {
        CompletableResultCode result = new CompletableResultCode();
        if (null != meterProvider) {
            synchronized (lock) {
                result = OpenTelemetryUtils.stopMeterProvider(meterProvider, true);
            }
        } else {
            result.succeed();
        }
        return result;
    }

    /**
     * Calls {@link SdkMeterProvider#forceFlush() meterProvider.forceFlush}
     *
     * @return The resulting {@link CompletableResultCode} completes when all complete.
     */
    public CompletableResultCode forceFlush() {
        return meterProvider != null ? meterProvider.forceFlush() : CompletableResultCode.ofSuccess();
    }

    /**
     * {@link SdkMeterProvider#forceFlush() flushes} the {@link #meterProvider} and waits for it to complete.
     */
    public void flush() {
        CompletableResultCode resultCode = forceFlush();
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
     * Registers {@link MeterProvider this} to {@link GlobalMeterProvider#set(MeterProvider)}
     */
    public MeterProvider registerGlobal() {
        GlobalMeterProvider.set(this);
        return this;
    }

    @Override
    public MeterBuilder meterBuilder(String instrumentationName) {
        return meterProvider.meterBuilder(instrumentationName);
    }

}
