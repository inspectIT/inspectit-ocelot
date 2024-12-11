package rocks.inspectit.ocelot.core.config.propertysources.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.IOException;
import java.time.Duration;

/**
 * Stores an instance of a {@link CloseableHttpClient} and it's relevant {@link HttpConfigSettings}.
 * Since the HTTP client is a rather expensive object, we create one instance and recreate it only
 * if the settings have changed.
 */
public class HttpClientHolder {

    private CloseableHttpClient httpClient;

    private Duration connectionTimeout;

    private Duration connectionRequestTimeout;

    private Duration socketTimeout;

    /**
     * Returns a {@link HttpClient}, which is used for fetching the configuration.
     * If the HTTP settings changed, a new client will be created and the old one will be closed.
     *
     * @return a {@link HttpClient} instance.
     */
    public CloseableHttpClient getHttpClient(HttpConfigSettings httpSettings) throws IOException {
        if(isUpdated(httpSettings) || httpClient == null) {
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

            RequestConfig config = configBuilder.build();

            if (httpClient != null) httpClient.close();
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            connectionTimeout = httpSettings.getConnectionTimeout();
            connectionRequestTimeout = httpSettings.getConnectionRequestTimeout();
            socketTimeout = httpSettings.getSocketTimeout();
        }
        return httpClient;
    }

    /**
     * @return true, if the provided settings differ from the active settings
     */
    private boolean isUpdated(HttpConfigSettings settings) {
        return settings.getConnectionTimeout() != connectionTimeout ||
                settings.getConnectionRequestTimeout() != connectionRequestTimeout ||
                settings.getSocketTimeout() != socketTimeout;
    }
}
