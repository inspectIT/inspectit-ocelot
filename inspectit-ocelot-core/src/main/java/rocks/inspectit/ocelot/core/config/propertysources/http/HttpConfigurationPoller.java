package rocks.inspectit.ocelot.core.config.propertysources.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.util.PropertyUtils;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for continuously triggering the updated of a agent configuration via HTTP.
 */
@Service
@Slf4j
public class HttpConfigurationPoller extends DynamicallyActivatableService implements Runnable {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ScheduledExecutorService executor;

    /**
     * The scheduled task.
     */
    private ScheduledFuture<?> pollerFuture;

    /**
     * The state of the used HTTP property source configuration.
     */
    private HttpPropertySourceState currentState;

    public HttpConfigurationPoller() {
        super("config.http");
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

        long frequencyMs = httpSettings.getFrequency().toMillis();
        pollerFuture = executor.scheduleWithFixedDelay(this, frequencyMs, frequencyMs, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping HTTP configuration polling service.");
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
        boolean wasUpdated = currentState.update();
        if (wasUpdated) {
            env.updatePropertySources(propertySources -> {
                if (propertySources.contains(InspectitEnvironment.HTTP_BASED_CONFIGURATION)) {
                    propertySources.replace(InspectitEnvironment.HTTP_BASED_CONFIGURATION, currentState.getCurrentPropertySource());
                } else {
                    propertySources.addBefore(InspectitEnvironment.DEFAULT_CONFIG_PROPERTYSOURCE_NAME, currentState.getCurrentPropertySource());
                }
            });
        }
    }
}
