package rocks.inspectit.ocelot.core.utils;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

public final class RetryUtils {

    private RetryUtils() {
        // prevents initialization
    }

    public static Retry buildRetry(RetrySettings retrySettings, String retryName) {
        if (retrySettings != null) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(retrySettings.getMaxAttempts())
                    .intervalFunction(IntervalFunction
                            .ofExponentialRandomBackoff(
                                    retrySettings.getInitialIntervalMillis(),
                                    retrySettings.getMultiplier(),
                                    retrySettings.getRandomizationFactor()))
                    .build();
            return Retry.of(retryName, retryConfig);
        }
        return null;
    }
}
