package rocks.inspectit.ocelot.core.command;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for fetching agent commands.
 */
@Service
@Slf4j
public class AgentCommandService extends DynamicallyActivatableService implements Runnable {

    @Autowired
    private ScheduledExecutorService executor;

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private HttpCommandFetcher commandFetcher;

    /**
     * The scheduled task.
     */
    private ScheduledFuture<?> handlerFuture;

    public AgentCommandService() {
        super("agentCommands");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        AgentCommandSettings settings = configuration.getAgentCommands();
        // the feature has to be enabled
        if (!settings.isEnabled()) {
            return false;
        }

        // enable the feature if the url is based on the HTTP config URL OR the url is specified directly
        if (settings.isDeriveFromHttpConfigUrl()) {
            return true;
        } else {
            return settings.getUrl() != null;
        }
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Starting agent command polling service.");

        try {
            URI commandUri = getCommandUri(configuration);
            commandFetcher.setCommandUri(commandUri);
        } catch (Exception e) {
            log.error("Could not enable the agent command polling service.", e);
            return false;
        }

        AgentCommandSettings settings = configuration.getAgentCommands();
        long pollingIntervalMs = settings.getPollingInterval().toMillis();

        handlerFuture = executor.scheduleWithFixedDelay(this, pollingIntervalMs, pollingIntervalMs, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command polling service.");

        if (handlerFuture != null) {
            handlerFuture.cancel(true);
        }
        return true;
    }

    @Override
    public void run() {
        log.debug("Trying to fetch new agent commands.");
        try {
            commandHandler.nextCommand();
        } catch (Exception exception) {
            log.error("Error while fetching agent command.", exception);
        }
    }

    @VisibleForTesting
    URI getCommandUri(InspectitConfig configuration) throws URISyntaxException {
        AgentCommandSettings settings = configuration.getAgentCommands();

        if (settings.isDeriveFromHttpConfigUrl()) {
            URL url = configuration.getConfig().getHttp().getUrl();
            if (url == null) {
                throw new IllegalStateException("The URL cannot derived from the HTTP configuration URL because it is null.");
            }

            String urlBase = String.format("%s://%s", url.getProtocol(), url.getHost());

            int port = url.getPort();
            if (port != -1) {
                urlBase += ":" + port;
            }

            return URI.create(urlBase + "/api/v1/agent/command");
        } else {
            return settings.getUrl().toURI();
        }
    }
}
