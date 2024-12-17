package rocks.inspectit.ocelot.core.config.propertysources.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Stores an instance of a {@link CloseableHttpClient} and it's relevant {@link HttpConfigSettings}.
 * Since the HTTP client is a rather expensive object, we create one instance and recreate it only
 * if the settings have changed.
 */
@Slf4j
public class HttpClientHolder {

    private CloseableHttpClient httpClient;

    private Duration connectionTimeout;

    private Duration connectionRequestTimeout;

    private Duration socketTimeout;

    private Duration timeToLive;

    /**
     * Returns a {@link HttpClient}, which is used for fetching the configuration.
     * If the HTTP settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link HttpClient} instance.
     */
    public CloseableHttpClient getHttpClient(HttpConfigSettings httpSettings) throws IOException {
        if(isUpdated(httpSettings) || httpClient == null) {
            log.debug("Creating new HTTP client for HTTP configuration with settings {}", httpSettings);
            RequestConfig config = getRequestConfig(httpSettings);
            HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config);

            if (httpSettings.getTimeToLive() != null) {
                int timeToLive = (int) httpSettings.getTimeToLive().toMillis();
                builder.setConnectionTimeToLive(timeToLive, TimeUnit.MILLISECONDS);
            }

            if (httpClient != null) httpClient.close();
            httpClient = builder.build();
            connectionTimeout = httpSettings.getConnectionTimeout();
            connectionRequestTimeout = httpSettings.getConnectionRequestTimeout();
            socketTimeout = httpSettings.getSocketTimeout();
            timeToLive = httpSettings.getTimeToLive();
        }
        return httpClient;
    }

    /**
     * @param httpSettings the current HTTP settings
     * @return the derived request configuration
     */
    private static RequestConfig getRequestConfig(HttpConfigSettings httpSettings) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (httpSettings.getConnectionTimeout() != null) {
            int connectionTimeout = (int) httpSettings.getConnectionTimeout().toMillis();
            configBuilder = configBuilder.setConnectTimeout(connectionTimeout);
        }
        if (httpSettings.getConnectionRequestTimeout() != null) {
            int connectionRequestTimeout = (int) httpSettings.getConnectionRequestTimeout().toMillis();
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (httpSettings.getSocketTimeout() != null) {
            int socketTimeout = (int) httpSettings.getSocketTimeout().toMillis();
            configBuilder = configBuilder.setSocketTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    /**
     * @return true, if the provided settings differ from the active settings
     */
    private boolean isUpdated(HttpConfigSettings settings) {
        return settings.getConnectionTimeout() != connectionTimeout ||
                settings.getConnectionRequestTimeout() != connectionRequestTimeout ||
                settings.getSocketTimeout() != socketTimeout ||
                settings.getTimeToLive() != timeToLive;
    }
}
