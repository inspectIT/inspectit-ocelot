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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for continuously polling a agent configuration via HTTP.
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
     * The currently used settings.
     */
    private HttpConfigSettings currentSettings;

    /**
     * The value of the latest 'Last-Modified' header.
     */
    private String lastModified;

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

        lastModified = null;

        currentSettings = configuration.getConfig().getHttp();
        long frequencyMs = currentSettings.getFrequency().toMillis();

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
     * Fetching the configuration and processing it.
     * If a new configuration has been fetched, the corresponding property source will be updated.
     */
    @Override
    public void run() {
        log.debug("Poll configuration via HTTP from URL: " + currentSettings.getUrl().toString());

        String configuration = fetchConfiguration();
        if (configuration != null) {
            try {
                Properties properties = PropertyUtils.readJson(configuration);
                final PropertiesPropertySource httpPropertySource = new PropertiesPropertySource(InspectitEnvironment.HTTP_BASED_CONFIGURATION, properties);

                env.updatePropertySources(propertySources -> {
                    if (propertySources.contains(InspectitEnvironment.HTTP_BASED_CONFIGURATION)) {
                        propertySources.replace(InspectitEnvironment.HTTP_BASED_CONFIGURATION, httpPropertySource);
                    } else {
                        propertySources.addBefore(InspectitEnvironment.DEFAULT_CONFIG_PROPERTYSOURCE_NAME, httpPropertySource);
                    }
                });
            } catch (Exception e) {
                log.error("Could not parse fetched configuration.", e);
            }
        }
    }

    /**
     * Creates the {@link HttpClient} which is used for fetching the configuration.
     *
     * @return A new {@link HttpClient} instance.
     */
    private HttpClient createHttpClient() {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (currentSettings.getConnectionTimeout() != null) {
            int connectionTimeout = (int) currentSettings.getConnectionTimeout().toMillis();
            configBuilder = configBuilder.setConnectTimeout(connectionTimeout);
        }
        if (currentSettings.getSocketTimeout() != null) {
            int socketTimeout = (int) currentSettings.getSocketTimeout().toMillis();
            configBuilder = configBuilder.setSocketTimeout(socketTimeout);
        }

        RequestConfig config = configBuilder.build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    /**
     * Fetches the configuration by executing a HTTP request against the configured HTTP endpoint. The request contains
     * the 'If-Modified-Since' header if a previous response returned a 'Last-Modified' header.
     *
     * @return The requests response body representing the configuration in a JSON format. null is returned if request fails or the
     * server returns 304 (not modified).
     */
    private String fetchConfiguration() {
        HttpGet httpGet = new HttpGet(currentSettings.getUrl().toString());
        if (lastModified != null) {
            httpGet.setHeader("If-Modified-Since", lastModified);
        }

        try {
            HttpResponse response = createHttpClient().execute(httpGet);
            return processHttpResponse(response);
        } catch (ClientProtocolException e) {
            log.error("HTTP protocol error occurred while fetching configuration.", e);
        } catch (IOException e) {
            log.error("A connection problem occurred while fetching configuration.", e);
        } catch (Exception e) {
            log.error("Exception occurred while fetching configuration.", e);
        } finally {
            httpGet.releaseConnection();
        }
        return null;
    }

    /**
     * Processes the response of a send request and extracts its body in case the status code is 200.
     * If the response contains a 'Last-Modified' header, its value will be stored.
     *
     * @param response the HTTP response object
     * @return the response body or null in case of an error or if the server sends 304 (not modified)
     * @throws IOException if an error occurs reading the input stream
     */
    private String processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            Header[] headers = response.getHeaders("Last-Modified");
            if (headers.length > 0) {
                lastModified = headers[0].getValue();
            }

            log.debug("Configuration has successfully been fetched.");

            return responseBody;
        } else if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
            log.debug("Server returned 304 - configuration has not been changed since the last time.");
            return null;
        } else {
            log.warn("Server returned an unexpected status code: " + statusCode);
            return null;
        }
    }
}
