package rocks.inspectit.ocelot.core.command.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Stores the instances of {@link CloseableHttpClient}s and their relevant {@link AgentCommandSettings}.
 * Since HTTP clients are rather expensive objects, we create one instance and recreate it only
 * if the settings have changed. To fetch agent commands we use two different HTTP clients.
 */
@Slf4j
public class CommandHttpClientHolder {

    /**
     * Http client used in the (normal) discovery mode.
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
     * Returns a {@link CloseableHttpClient}, which is used for fetching agent commands.
     * If the agent-command settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link CloseableHttpClient} instance for discovery-mode
     */
    public CloseableHttpClient getDiscoveryHttpClient(AgentCommandSettings settings) throws IOException {
        if(discoveryHttpClient == null || isDiscoveryUpdated(settings)) {
            log.debug("Creating new HTTP client for discovery agent commands with settings {}", settings);
            RequestConfig config = getDiscoveryRequestConfig(settings);
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config);

            if (settings.getTimeToLive() != null || settings.getConnectionTimeout() != null) {
                HttpClientConnectionManager connectionManager = getDiscoveryConnectionManager(settings);
                builder.setConnectionManager(connectionManager);
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
     * Returns a {@link CloseableHttpClient}, which is used for fetching agent commands.
     * If the agent-command settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link CloseableHttpClient} instance for live-mode
     */
    public CloseableHttpClient getLiveHttpClient(AgentCommandSettings settings) throws IOException {
        if(liveHttpClient == null || isLiveUpdated(settings)) {
            log.debug("Creating new HTTP client for live agent commands with settings {}", settings);
            RequestConfig config = getLiveRequestConfig(settings);
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config);

            if (settings.getTimeToLive() != null || settings.getLiveConnectionTimeout() != null) {
                HttpClientConnectionManager connectionManager = getLiveConnectionManager(settings);
                builder.setConnectionManager(connectionManager);
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
     * @return the derived connection manager for discovery-mode
     */
    private static HttpClientConnectionManager getDiscoveryConnectionManager(AgentCommandSettings settings) {
        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.custom();

        if(settings.getConnectionTimeout() != null) {
            Timeout connectTimeout = convertToTimeout(settings.getConnectionTimeout());
            connectionBuilder.setConnectTimeout(connectTimeout);
        }

        if(settings.getTimeToLive() != null) {
            connectionBuilder.setTimeToLive(settings.getTimeToLive().toMillis(), TimeUnit.MILLISECONDS);
        }

        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionBuilder.build())
                .build();
    }

    /**
     * @param settings the current agent-command settings
     * @return the derived connection manager for live-mode
     */
    private static HttpClientConnectionManager getLiveConnectionManager(AgentCommandSettings settings) {
        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.custom();

        if(settings.getLiveConnectionRequestTimeout() != null) {
            Timeout connectTimeout = convertToTimeout(settings.getLiveConnectionTimeout());
            connectionBuilder.setConnectTimeout(connectTimeout);
        }

        if(settings.getTimeToLive() != null) {
            connectionBuilder.setTimeToLive(settings.getTimeToLive().toMillis(), TimeUnit.MILLISECONDS);
        }

        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionBuilder.build())
                .build();
    }

    /**
     * @param settings the current agent-command settings
     * @return the derived request configuration for discovery-mode
     */
    private static RequestConfig getDiscoveryRequestConfig(AgentCommandSettings settings) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (settings.getConnectionRequestTimeout() != null) {
            Timeout connectionRequestTimeout = convertToTimeout(settings.getConnectionRequestTimeout());
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (settings.getSocketTimeout() != null) {
            Timeout socketTimeout =  convertToTimeout(settings.getSocketTimeout());
            configBuilder = configBuilder.setResponseTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    /**
     * @param settings the current agent-command settings
     * @return the derived request configuration for live-mode
     */
    private static RequestConfig getLiveRequestConfig(AgentCommandSettings settings) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (settings.getLiveConnectionRequestTimeout() != null) {
            Timeout connectionRequestTimeout = convertToTimeout(settings.getLiveConnectionRequestTimeout());
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (settings.getLiveSocketTimeout() != null) {
            Timeout socketTimeout =  convertToTimeout(settings.getLiveSocketTimeout());
            configBuilder = configBuilder.setResponseTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    private static Timeout convertToTimeout(Duration duration) {
        return Timeout.of(duration.toMillis(), TimeUnit.MILLISECONDS);
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
