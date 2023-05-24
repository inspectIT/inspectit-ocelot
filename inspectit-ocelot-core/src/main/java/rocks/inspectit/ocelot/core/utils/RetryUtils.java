package rocks.inspectit.ocelot.core.utils;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

import java.time.temporal.ChronoUnit;

public final class RetryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryUtils.class);

    private RetryUtils() {
        // prevents initialization
    }

    public static Retry buildRetry(RetrySettings retrySettings, String retryName) {
        if (retrySettings != null) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(retrySettings.getMaxAttempts())
                    .intervalFunction(IntervalFunction
                            .ofExponentialRandomBackoff(
                                    retrySettings.getInitialInterval(),
                                    retrySettings.getMultiplier().doubleValue(),
                                    retrySettings.getRandomizationFactor().doubleValue()))
                    .build();
            Retry retry = Retry.of(retryName, retryConfig);
            retry.getEventPublisher().onRetry( event -> LOGGER.info("Retrying for {} in {}.", retryName, event.getWaitInterval()));
            return retry;
        }
        return null;
    }
}
