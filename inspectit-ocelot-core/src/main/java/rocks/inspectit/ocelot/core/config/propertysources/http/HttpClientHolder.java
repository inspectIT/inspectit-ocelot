package rocks.inspectit.ocelot.core.config.propertysources.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
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
        if(httpClient == null || isUpdated(httpSettings)) {
            log.debug("Creating new HTTP client for HTTP configuration with settings {}", httpSettings);
            RequestConfig config = getRequestConfig(httpSettings);
            HttpClientBuilder builder = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config);

            if (httpSettings.getTimeToLive() != null || httpSettings.getConnectionTimeout() != null) {
                HttpClientConnectionManager connectionManager = getConnectionManager(httpSettings);
                builder.setConnectionManager(connectionManager);
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

        if (httpSettings.getConnectionRequestTimeout() != null) {
            Timeout connectionRequestTimeout = convertToTimeout(httpSettings.getConnectionRequestTimeout());
            configBuilder = configBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        if (httpSettings.getSocketTimeout() != null) {
            Timeout socketTimeout =  convertToTimeout(httpSettings.getSocketTimeout());
            configBuilder = configBuilder.setResponseTimeout(socketTimeout);
        }

        return configBuilder.build();
    }

    /**
     * @param httpSettings the current HTTP settings
     * @return the derived connection manager
     */
    private static HttpClientConnectionManager getConnectionManager(HttpConfigSettings httpSettings) {
        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.custom();

        if(httpSettings.getConnectionTimeout() != null) {
            Timeout connectTimeout = convertToTimeout(httpSettings.getConnectionTimeout());
            connectionBuilder.setConnectTimeout(connectTimeout);
        }

        if(httpSettings.getTimeToLive() != null) {
            connectionBuilder.setTimeToLive(httpSettings.getTimeToLive().toMillis(), TimeUnit.MILLISECONDS);
        }

        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionBuilder.build())
                .build();
    }

    private static Timeout convertToTimeout(Duration duration) {
        return Timeout.of(duration.toMillis(), TimeUnit.MILLISECONDS);
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
