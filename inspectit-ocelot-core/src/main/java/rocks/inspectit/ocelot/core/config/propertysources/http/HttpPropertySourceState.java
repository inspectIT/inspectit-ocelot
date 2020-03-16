package rocks.inspectit.ocelot.core.config.propertysources.http;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.IAgent;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.core.config.util.PropertyUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Properties;

/**
 * Representing and managing the state of a HTTP-based agent configuration.
 */
@Slf4j
public class HttpPropertySourceState {

    /**
     * The prefix which is used for the meta information HTTP headers.
     */
    private static final String META_HEADER_PREFIX = "X-OCELOT-";

    /**
     * Used in case the properties fetched via HTTP are empty.
     */
    private static final Properties EMPTY_PROPERTIES = new Properties();

    /**
     * The name used for the property source.
     */
    @Getter
    private String name;

    /**
     * The currently used settings.
     */
    private HttpConfigSettings currentSettings;

    /**
     * The value of the latest 'Last-Modified' header.
     */
    private String latestLastModified;

    /**
     * The value of the latest 'ETag' header.
     */
    private String latestETag;

    /**
     * The latest property source.
     */
    @Getter
    private PropertySource currentPropertySource;

    /**
     * Number of unsuccessful connection attempts.
     */
    private int errorCounter;

    /**
     * Flag indicates that it is the first attempt to write the configuration to file.
     * See {@link #writePersistenceFile(String)}
     */
    private boolean firstFileWriteAttempt = true;

    /**
     * Flag indicates if first attempt to write configuration to file was successful.
     * If this resolves to false no further attempts are performed.
     * See {@link #writePersistenceFile(String)}
     */
    @Getter
    private boolean firstFileWriteAttemptSuccessful = true;

    /**
     * Constructor.
     *
     * @param name            the name used for the property source
     * @param currentSettings the settings used to fetch the configuration
     */
    public HttpPropertySourceState(String name, HttpConfigSettings currentSettings) {
        this.name = name;
        this.currentSettings = currentSettings;
        this.errorCounter = 0;
        //ensure that currentPropertySource is never null, even if the initial fetching fails
        currentPropertySource = new PropertiesPropertySource(name, new Properties());
    }

    /**
     * Fetches the latest configuration. If the configuration is successfully fetched a new {@link PropertySource} is
     * created which can be accessed using {@link #getCurrentPropertySource()}. In case of an error or if the server responds
     * that the configuration has not be changed the property source will not be updated!
     *
     * @param fallBackToFile if true, the configured persisted configuration will be loaded in case of an error
     * @return returns true if a new property source has been created, otherwise false.
     */
    public boolean update(boolean fallBackToFile) {
        String configuration = fetchConfiguration(fallBackToFile);
        if (configuration != null) {
            try {
                Properties properties = parseProperties(configuration);
                currentPropertySource = new PropertiesPropertySource(name, properties);
                return true;
            } catch (Exception e) {
                log.error("Could not parse fetched configuration.", e);
            }
        }

        return false;
    }

    /**
     * Parse the given properties string into an instance of {@link Properties}. The string can be represented as a JSON
     * or YAML document.
     *
     * @param rawProperties the properties in a String representation
     * @return the parsed {@link Properties} object
     */
    private Properties parseProperties(String rawProperties) {
        if (StringUtils.isBlank(rawProperties)) {
            return EMPTY_PROPERTIES;
        }
        try {
            return PropertyUtils.readJson(rawProperties);
        } catch (IOException e) {
            return PropertyUtils.readYaml(rawProperties);
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
    private String fetchConfiguration(boolean fallBackToFile) {
        HttpGet httpGet;
        try {
            URI uri = getEffectiveRequestUri();
            log.debug("Updating configuration via HTTP from URL: {}", uri.toString());
            httpGet = new HttpGet(uri);
        } catch (URISyntaxException e) {
            log.error("Error building HTTP URI for fetching configuration!", e);
            return null;
        }

        if (latestLastModified != null) {
            httpGet.setHeader("If-Modified-Since", latestLastModified);
        }
        if (latestETag != null) {
            httpGet.setHeader("If-None-Match", latestETag);
        }

        setAgentMetaHeaders(httpGet);

        String configuration = null;
        boolean isError = true;
        try {
            HttpResponse response = createHttpClient().execute(httpGet);
            configuration = processHttpResponse(response);
            isError = false;
            if (errorCounter != 0) {
                log.info("Configuration fetch has been successful after {} unsuccessful attempts.", errorCounter);
                errorCounter = 0;
            }
        } catch (ClientProtocolException e) {
            logFetchError("HTTP protocol error occurred while fetching configuration.", e);
        } catch (IOException e) {
            logFetchError("A IO problem occurred while fetching configuration.", e);
        } catch (Exception e) {
            logFetchError("Exception occurred while fetching configuration.", e);
        } finally {
            httpGet.releaseConnection();
        }

        if (!isError && configuration != null) {
            writePersistenceFile(configuration);
        } else if (isError && fallBackToFile) {
            configuration = readPersistenceFile();
        }

        return configuration;
    }

    /**
     * Injects all the agent's meta information headers, which should be send when fetching a new configuration,
     * into the given request request.
     *
     * @param httpGet the request to inject the meat information headers
     */
    private void setAgentMetaHeaders(HttpGet httpGet) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        httpGet.setHeader(META_HEADER_PREFIX + "AGENT-VERSION", AgentManager.getAgentVersion());
        httpGet.setHeader(META_HEADER_PREFIX + "JAVA-VERSION", System.getProperty("java.version"));
        httpGet.setHeader(META_HEADER_PREFIX + "VM-NAME", runtime.getVmName());
        httpGet.setHeader(META_HEADER_PREFIX + "VM-VERSION", runtime.getVmVendor());
        httpGet.setHeader(META_HEADER_PREFIX + "START-TIME", String.valueOf(runtime.getStartTime()));
    }

    /**
     * Increments the errorCounter and prints ERROR log if the errorCounter is power of two
     *
     * @param message   error message to log
     * @param exception exception that occurred when trying to fetch a configuration
     */
    private void logFetchError(String message, Exception exception) {
        errorCounter++;
        //check if errorCounter is a power of 2
        if (((errorCounter & (errorCounter - 1)) == 0)) {
            log.error(message, exception);
        }
    }

    /**
     * Builds the request URI by combining the base URI with the configured attributes.
     *
     * @return the resulting URI
     * @throws URISyntaxException if the base URI is malformed
     */
    public URI getEffectiveRequestUri() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(currentSettings.getUrl().toURI());
        currentSettings.getAttributes().entrySet().stream()
                .filter(pair -> !StringUtils.isEmpty(pair.getValue()))
                .forEach(pair -> uriBuilder.setParameter(pair.getKey(), pair.getValue()));
        return uriBuilder.build();
    }

    /**
     * Processes the response of a send request and extracts its body in case the status code is 200.
     * If the response contains a 'Last-Modified' header, its value will be stored.
     *
     * @param response the HTTP response object
     * @return the response body or null in case server sends 304 (not modified)
     * @throws IOException if an error occurs reading the input stream or if the server returned an unexpected status code
     */
    private String processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            Header[] headersLastModified = response.getHeaders("Last-Modified");
            if (headersLastModified.length > 0) {
                latestLastModified = headersLastModified[0].getValue();
            } else {
                latestLastModified = null;
            }

            Header[] headersETag = response.getHeaders("ETag");
            if (headersETag.length > 0) {
                latestETag = headersETag[0].getValue();
            } else {
                latestETag = null;
            }

            log.info("HTTP Configuration has successfully been fetched.");

            return responseBody;
        } else if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
            log.debug("Server returned 304 - configuration has not been changed since the last time.");
            return null;
        } else {
            throw new IOException("Server returned an unexpected status code: " + statusCode);
        }
    }

    /**
     * Writes the given content to the file specified via {@link HttpConfigSettings#getPersistenceFile()}.
     *
     * @param content the content to write to the file (normally the most recent configuration)
     */
    void writePersistenceFile(String content) {
        if (firstFileWriteAttemptSuccessful) {
            try {
                String file = currentSettings.getPersistenceFile();
                if (!StringUtils.isBlank(file)) {
                    log.debug("Writing HTTP Configuration persistence file '{}'", file);
                    Path path = Paths.get(file);
                    Files.createDirectories(path.getParent());
                    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                if (firstFileWriteAttempt) {
                    firstFileWriteAttemptSuccessful = false;
                }
                log.error("Could not write persistence file for HTTP-configuration.", e);
            }
            firstFileWriteAttempt = false;
        }
    }

    /**
     * Attempts to read the file specified via {@link HttpConfigSettings#getPersistenceFile()}.
     *
     * @return the content of the file (if it exists, otherwise null
     */
    private String readPersistenceFile() {
        String file = currentSettings.getPersistenceFile();
        if (!StringUtils.isBlank(file)) {
            Path path = Paths.get(file);
            if (Files.exists(path)) {
                try {
                    byte[] content = Files.readAllBytes(path);
                    FileTime lastModified = Files.getLastModifiedTime(path);
                    log.info("Loading HTTP Configuration persistence file '{}' from {} as fallback", file, new Date(lastModified.toMillis()));
                    return new String(content, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.error("Error loading HTTP Configuration persistence file '{}'", file, e);
                }
            } else {
                log.warn("HTTP Configuration persistence file '{}' not found, using default configuration", file);
            }
        }
        return null;
    }


}
