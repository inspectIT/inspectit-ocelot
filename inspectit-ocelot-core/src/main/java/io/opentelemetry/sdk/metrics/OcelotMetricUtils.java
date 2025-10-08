package io.opentelemetry.sdk.metrics;

import io.opentelemetry.api.metrics.Meter;

public class OcelotMetricUtils {

    private static final Class<? extends Meter> SDKMETER_CLASS;
    
    static {
        try {
            SDKMETER_CLASS = (Class<Meter>) Class.forName("io.opentelemetry.sdk.metrics.SdkMeter");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * If the meter is not a {@link SdkMeter}, we expect a NOOP-Meter.
     *
     * @return true, if the provided meter is an {@link SdkMeter}
     */
    public static boolean isSdkMeter(Meter meter) {
        return SDKMETER_CLASS.isInstance(meter);
    }
}
