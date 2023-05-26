package rocks.inspectit.ocelot.core.utils;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetryUtilsTest {

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
