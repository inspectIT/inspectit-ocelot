package rocks.inspectit.ocelot.core.utils;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryUtilsTest {

    @Nested
    class RetryTest {

        @Test
        void returnsRetryIfEnabled() {
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setEnabled(true);
            retrySettings.setMaxAttempts(5);
            retrySettings.setInitialInterval(Duration.ofMillis(5000));
            retrySettings.setMultiplier(BigDecimal.TEN);
            retrySettings.setRandomizationFactor(BigDecimal.valueOf(0.5));

            Retry retry = RetryUtils.buildRetry(retrySettings, "anyName");

            assertThat(retry).isNotNull();
        }

        @Test
        void returnsNullIfRetrySettingsIsNull() {
            Retry retry = RetryUtils.buildRetry(null, "anyName");

            assertThat(retry).isNull();
        }

        @Test
        void returnsNullForDefaultRetrySettings() {
            Retry retry = RetryUtils.buildRetry(new RetrySettings(), "anyName");

            assertThat(retry).isNull();
        }

        @Test
        void returnsNullIfRetriesAreDisabled() {
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setEnabled(false);

            Retry retry = RetryUtils.buildRetry(retrySettings, "anyName");

            assertThat(retry).isNull();
        }
    }

    @Nested
    class TimeLimiterTest {

        @Test
        void returnsTimeLimiterIfEnabled() {
            Duration timeLimit = Duration.ofMillis(500);
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setEnabled(true);
            retrySettings.setTimeLimit(timeLimit);

            TimeLimiter timeLimiter = RetryUtils.buildTimeLimiter(retrySettings, "anyName");

            assertThat(timeLimiter).isNotNull();
            assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(timeLimit);
        }

        @Test
        void returnsNullIfRetrySettingsIsNull() {
            TimeLimiter timeLimiter = RetryUtils.buildTimeLimiter(null, "anyName");

            assertThat(timeLimiter).isNull();
        }

        @Test
        void returnsNullForDefaultRetrySettings() {
            TimeLimiter timeLimiter = RetryUtils.buildTimeLimiter(new RetrySettings(), "anyName");

            assertThat(timeLimiter).isNull();
        }

        @Test
        void returnsNullIfRetriesAreDisabled() {
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setEnabled(false);

            TimeLimiter timeLimiter = RetryUtils.buildTimeLimiter(retrySettings, "anyName");

            assertThat(timeLimiter).isNull();
        }
    }
}
