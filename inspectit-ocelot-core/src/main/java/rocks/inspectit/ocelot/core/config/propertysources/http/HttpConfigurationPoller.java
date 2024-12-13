package rocks.inspectit.ocelot.core.config.propertysources.http;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.commons.models.health.AgentHealthState;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for continuously triggering the updating of an agent configuration via HTTP.
 */
@Service
@Slf4j
public class HttpConfigurationPoller extends DynamicallyActivatableService implements Runnable {

    @Autowired
    private ScheduledExecutorService executor;

    /**
     * The scheduled task.
     */
    private ScheduledFuture<?> pollerFuture;

    /**
     * The interval for the scheduled task.
     */
    private Duration pollingInterval;

    /**
     * The executor to cancel the polling task by timeout. This should prevent the HTTP thread to deadlock.
     */
    private final TaskTimeoutExecutor timeoutExecutor;

    /**
     * The maximum time to run one polling task.
     */
    private Duration pollingTimeout;

    /**
     * The state of the used HTTP property source configuration.
     */
    @Getter
    private HttpPropertySourceState currentState;

    public HttpConfigurationPoller() {
        super("config.http");
        timeoutExecutor = new TaskTimeoutExecutor();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return configuration.getConfig().getHttp().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Starting HTTP configuration polling service.");

        HttpConfigSettings httpSettings = configuration.getConfig().getHttp();

        currentState = new HttpPropertySourceState(InspectitEnvironment.HTTP_BASED_CONFIGURATION, httpSettings);

        pollingInterval = httpSettings.getFrequency();
        pollingTimeout = httpSettings.getTaskTimeout();
        startScheduledPolling();

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping HTTP configuration polling service.");
        cancelTimeout();
        if (pollerFuture != null) {
            pollerFuture.cancel(true);
        }
        return true;
    }

    /**
     * Triggering the update of the {@link #currentState}. If the HTTP property source state has been updated, the updated
     * property source will be activated by adding it to the environment.
     */
    @Override
    public void run() {
        log.debug("Updating HTTP property source.");
        // Fetch configuration
        boolean wasUpdated = currentState.update(false);
        // After the configuration was fetched, the task should no longer timeout
        cancelTimeout();
        if (wasUpdated) {
            env.updatePropertySources(propertySources -> {
                if (propertySources.contains(InspectitEnvironment.HTTP_BASED_CONFIGURATION)) {
                    propertySources.replace(InspectitEnvironment.HTTP_BASED_CONFIGURATION, currentState.getCurrentPropertySource());
                }
            });
        }
    }

    /**
     * Start the scheduled HTTP polling.
     */
    private void startScheduledPolling() {
        pollerFuture = executor.scheduleWithFixedDelay(this,
                pollingInterval.toMillis(), pollingInterval.toMillis(), TimeUnit.MILLISECONDS);
        // Setup timeout for fetching the configuration
        if (pollingTimeout != null)
            timeoutExecutor.scheduleCancelling(pollerFuture, "http.config", this::startScheduledPolling, pollingTimeout);
    }

    public void updateAgentHealthState(AgentHealthState agentHealth) {
        if (currentState != null) {
            currentState.updateAgentHealthState(agentHealth);
        }
    }

    public AgentHealthState getCurrentAgentHealthState() {
        if(currentState == null) return null;
        return currentState.getAgentHealth();
    }

    @VisibleForTesting
    void cancelTimeout() {
        if (timeoutExecutor != null) {
            timeoutExecutor.cancelTimeout();
        }
    }
}
