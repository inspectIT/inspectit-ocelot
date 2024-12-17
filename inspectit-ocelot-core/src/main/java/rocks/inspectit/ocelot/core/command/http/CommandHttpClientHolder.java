package rocks.inspectit.ocelot.core.command.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Stores the instances of {@link CloseableHttpClient} and their relevant {@link AgentCommandSettings}.
 * Since HTTP clients are rather expensive objects, we create one instance and recreate it only
 * if the settings have changed. To fetch agent commands we use two different HTTP clients.
 */
@Slf4j
public class CommandHttpClientHolder {

    /**
     * Http client used in the normal mode.
     */
    private CloseableHttpClient discoveryHttpClient;

    private Duration discoveryConnectionTimeout;

    private Duration discoveryConnectionRequestTimeout;

    private Duration discoverySocketTimeout;

    /**
     * Http client used in the live mode (longer timeouts).
     */
    private CloseableHttpClient liveHttpClient;

    private Duration liveConnectionTimeout;

    private Duration liveConnectionRequestTimeout;

    private Duration liveSocketTimeout;

    private Duration timeToLive;

    /**
     * Returns a {@link HttpClient}, which is used for fetching agent commands.
     * If the agent-command settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link HttpClient} instance for discovery-mode
     */
    public CloseableHttpClient getDiscoveryHttpClient(AgentCommandSettings settings) throws IOException {
        if(isDiscoveryUpdated(settings) || discoveryHttpClient == null) {
            log.debug("Creating new HTTP client for discovery agent commands with settings {}", settings);
            RequestConfig config = getDiscoveryRequestConfig(settings);
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config);

            if (settings.getTimeToLive() != null) {
                int timeToLive = (int) settings.getTimeToLive().toMillis();
                builder.setConnectionTimeToLive(timeToLive, TimeUnit.MILLISECONDS);
            }

            if (discoveryHttpClient != null) discoveryHttpClient.close();
            discoveryHttpClient = builder.build();
            discoveryConnectionTimeout = settings.getConnectionTimeout();
            discoveryConnectionRequestTimeout = settings.getConnectionRequestTimeout();
            discoverySocketTimeout = settings.getSocketTimeout();
            timeToLive = settings.getTimeToLive();
        }
        return discoveryHttpClient;
    }

    /**
     * Returns a {@link HttpClient}, which is used for fetching agent commands.
     * If the agent-command settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link HttpClient} instance for live-mode
     */
    public CloseableHttpClient getLiveHttpClient(AgentCommandSettings settings) throws IOException {
        if(isLiveUpdated(settings) || liveHttpClient == null) {
            log.debug("Creating new HTTP client for live agent commands with settings {}", settings);
            RequestConfig config = getLiveRequestConfig(settings);
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config);

            if (settings.getTimeToLive() != null) {
                int timeToLive = (int) settings.getTimeToLive().toMillis();
                builder.setConnectionTimeToLive(timeToLive, TimeUnit.MILLISECONDS);
            }

            if (liveHttpClient != null) liveHttpClient.close();
            liveHttpClient = builder.build();
            liveConnectionTimeout = settings.getLiveConnectionTimeout();
            liveConnectionRequestTimeout = settings.getLiveConnectionRequestTimeout();
            liveSocketTimeout = settings.getLiveSocketTimeout();
            timeToLive = settings.getTimeToLive();
        }
        return liveHttpClient;
    }

    /**
     * @param settings the current agent-command settings
     * @return the derived request configuration for discovery-mode
     */
    private static RequestConfig getDiscoveryRequestConfig(AgentCommandSettings settings) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (settings.getConnectionTimeout() != null) {
            int connectionTimeout = (int) settings.getConnectionTimeout().toMillis();
            configBuilder = configBuilder.setConnectTimeout(connectionTimeout);
        }
        if (settings.getConnectionRequestTimeout() != null) {
            int connectionRequestTimeout = (int) settings.getConnectionRequestTimeout().toMillis();
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (settings.getSocketTimeout() != null) {
            int socketTimeout = (int) settings.getSocketTimeout().toMillis();
            configBuilder = configBuilder.setSocketTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    /**
     * @param settings the current agent-command settings
     * @return the derived request configuration for live-mode
     */
    private static RequestConfig getLiveRequestConfig(AgentCommandSettings settings) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (settings.getConnectionTimeout() != null) {
            int connectionTimeout = (int) settings.getLiveConnectionTimeout().toMillis();
            configBuilder = configBuilder.setConnectTimeout(connectionTimeout);
        }
        if (settings.getConnectionRequestTimeout() != null) {
            int connectionRequestTimeout = (int) settings.getLiveConnectionRequestTimeout().toMillis();
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (settings.getSocketTimeout() != null) {
            int socketTimeout = (int) settings.getLiveSocketTimeout().toMillis();
            configBuilder = configBuilder.setSocketTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    /**
     * @return true, if the provided settings differ from the active settings for discovery-mode
     */
    private boolean isDiscoveryUpdated(AgentCommandSettings settings) {
        return settings.getConnectionTimeout() != discoveryConnectionTimeout ||
                settings.getConnectionRequestTimeout() != discoveryConnectionRequestTimeout ||
                settings.getSocketTimeout() != discoverySocketTimeout ||
                settings.getTimeToLive() != timeToLive;
    }

    /**
     * @return true, if the provided settings differ from the active settings
     */
    private boolean isLiveUpdated(AgentCommandSettings settings) {
        return settings.getLiveConnectionTimeout() != liveConnectionTimeout ||
                settings.getLiveConnectionRequestTimeout() != liveConnectionRequestTimeout ||
                settings.getLiveSocketTimeout() != liveSocketTimeout ||
                settings.getTimeToLive() != timeToLive;
    }
}
