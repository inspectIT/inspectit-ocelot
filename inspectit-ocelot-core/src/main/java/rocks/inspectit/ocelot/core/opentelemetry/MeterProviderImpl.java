package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

/**
 * Custom implementation of {@link MeterProvider} thats wraps {@link SdkMeterProvider}.
 * This wrapper is used to change {@link #meterProvider} without resetting {@link io.opentelemetry.api.metrics.GlobalMeterProvider#set(MeterProvider)}.
 * For use, create the {@link MeterProviderImpl} and set it once to {@link io.opentelemetry.api.metrics.GlobalMeterProvider#set(MeterProvider)}. If the {@link SdkMeterProvider} changes, simply
 */
@Slf4j
@Builder
public class MeterProviderImpl implements MeterProvider {

    private SdkMeterProvider meterProvider;

    @Builder.Default
    private Object lock = new Object();

    /**
     * Registers the {@link SdkMeterProvider}. If an instance of {@link SdkMeterProvider} was already registered, it is {@link SdkMeterProvider#forceFlush() flushed} and {@link SdkMeterProvider#shutdown() shutdown} before the new {@link SdkMeterProvider} is set to {@link #meterProvider}
     *
     * @param meterProvider
     */
    public void set(SdkMeterProvider meterProvider) {
        synchronized (lock) {
            // shut down previous meterProvider if set
            if (null != this.meterProvider) {
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
     * Shuts down the currently registered {@link #meterProvider}
     *
     * @return
     */
    public synchronized CompletableResultCode shutdown() {
        CompletableResultCode result;
        synchronized (lock) {
            result = OpenTelemetryUtils.stopMeterProvider(meterProvider, true);
        }
        return result;
    }

    /**
     * Registers {@link SdkMeterProvider this} to {@link GlobalMeterProvider#set(MeterProvider)}
     */
    public MeterProvider registerGlobal() {
        GlobalMeterProvider.set(this.meterProvider);
        return this;
    }

    @Override
    public MeterBuilder meterBuilder(String instrumentationName) {
        return this.meterProvider.meterBuilder(instrumentationName);
    }
}
