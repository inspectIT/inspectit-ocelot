package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HttpCommandPoller extends DynamicallyActivatableService implements Runnable {

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
    private HttpCommandHandler commandHandler;

    @VisibleForTesting
    long DEFAULT_POLLING_INTERVAL_MS = 10000;

    public HttpCommandPoller() {
        super("config.http");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return configuration.getConfig().getHttp().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Starting HTTP command polling service.");

        HttpConfigSettings httpSettings = configuration.getConfig().getHttp();

        commandHandler = new HttpCommandHandler(httpSettings);

        pollerFuture = executor.scheduleWithFixedDelay(this, DEFAULT_POLLING_INTERVAL_MS, DEFAULT_POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping HTTP command polling service.");
        if (pollerFuture != null) {
            pollerFuture.cancel(true);
        }
        return true;
    }

    @Override
    public void run() {
        log.debug("Updating HTTP property source.");
        try {
            commandHandler.fetchCommand();
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding Exception occurred while fetching command: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Could not process fetched json: " + e.getMessage());
        }
    }
}
