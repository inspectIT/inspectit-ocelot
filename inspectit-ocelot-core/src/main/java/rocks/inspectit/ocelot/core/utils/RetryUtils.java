package rocks.inspectit.ocelot.core.utils;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

public final class RetryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryUtils.class);

    private RetryUtils() {
        // prevents initialization
    }

    public static Retry buildRetry(RetrySettings retrySettings, String retryName) {
        if (useRetry(retrySettings)) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(retrySettings.getMaxAttempts())
                    .intervalFunction(IntervalFunction
                            .ofExponentialRandomBackoff(
                                    retrySettings.getInitialInterval(),
                                    retrySettings.getMultiplier().doubleValue(),
                                    retrySettings.getRandomizationFactor().doubleValue()))
                    .build();
            Retry retry = Retry.of(retryName, retryConfig);
            retry.getEventPublisher().onRetry(event -> LOGGER.info("Retrying for {} in {}.", retryName, event.getWaitInterval()));
            return retry;
        }
        return null;
    }

    public static TimeLimiter buildTimeLimiter(RetrySettings retrySettings, String timeLimiterName) {
        if(useTimeLimiter(retrySettings)) {
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .cancelRunningFuture(true)
                    .timeoutDuration(retrySettings.getTimeLimit())
                    .build();
            TimeLimiter timeLimiter = TimeLimiter.of(timeLimiterName, timeLimiterConfig);
            timeLimiter.getEventPublisher().onTimeout(event -> LOGGER.info("Time limit for {} was exceeded.", timeLimiterName));
            return timeLimiter;
        }
        return null;
    }

    private static boolean useRetry(RetrySettings retrySettings) {
        return retrySettings != null && retrySettings.isEnabled();
    }

    private static boolean useTimeLimiter(RetrySettings retrySettings) {
        return useRetry(retrySettings) && retrySettings.getTimeLimit() != null;
    }
}
