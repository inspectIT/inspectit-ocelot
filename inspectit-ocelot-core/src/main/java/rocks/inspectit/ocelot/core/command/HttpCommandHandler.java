package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class HttpCommandHandler {

    /**
     * Used to delegate recieved {@link Command} objects to their respective implementation of {@link rocks.inspectit.ocelot.core.command.handler.CommandExecutor}.
     */
    @VisibleForTesting
    @Autowired
    CommandDelegator commandDelegator;

    private final ObjectMapper objectMapper = new ObjectMapper().enableDefaultTyping();

    /**
     * The prefix which is used for the meta information HTTP headers.
     */
    private static final String META_HEADER_PREFIX = "X-OCELOT-";

    /**
     * The currently used settings.
     */
    @VisibleForTesting
    HttpConfigSettings currentSettings;

    /**
     * The default timeout duration in milliseconds.
     */
    private final int DEFAULT_TIMEOUT_DURATION = 30000;

    /**
     * The timeout duration in milliseconds when in discovery mode.
     */
    private int discoveryTimeoutDuration;

    /**
     * Counts how many times the request for a new {@link Command} have been repeated.
     */
    private int repeatCounter = 0;

    /**
     * Defines the maximum amount of repeats for a new {@link Command}.
     */
    private int maxRepeats = 3;

    /**
     * If true, the request for fetching a {@link Command} is immediately repeated.
     */
    private boolean repeat = false;

    /**
     * Used to update the current settings.
     *
     * @param currentSettings the settings used to fetch the configuration.
     */
    public void updateSettings(HttpConfigSettings currentSettings) {
        this.currentSettings = currentSettings;
        this.discoveryTimeoutDuration = currentSettings.getDiscoveryTimeOutDuration();
    }

    /**
     * Creates the {@link HttpClient} which is used for fetching commands.
     *
     * @return A new {@link HttpClient} instance.
     */
    private HttpClient createHttpClient(int timeOut) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        configBuilder = configBuilder.setSocketTimeout(timeOut);
        RequestConfig config = configBuilder.build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    /**
     * Fetches a {@link Command} by sending an empty payload. Uses the DEFAULT_TIMEOUT_DURATION as timeout value.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    public boolean fetchCommand() throws UnsupportedEncodingException, JsonProcessingException {
        return fetchCommand(null);
    }

    /**
     * Fetches a {@link Command} by sending the given parameter as payload. Uses the DEFAULT_TIMEOUT_DURATION as timeout value.
     *
     * @param payload The payload to be send.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    private boolean fetchCommand(CommandResponse payload) throws UnsupportedEncodingException, JsonProcessingException {
        return fetchCommand(payload, DEFAULT_TIMEOUT_DURATION);
    }

    /**
     * Fetches a {@link Command} by sending the given {@link CommandResponse} as payload and uses the given timeout-int as timeout.
     *
     * @param payload The payload to be send.
     * @param timeout The timeout to be used for the request.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    private boolean fetchCommand(CommandResponse payload, int timeout) throws JsonProcessingException, UnsupportedEncodingException {
        HttpPost httpPost;
        try {
            URI uri = this.currentSettings.getCommandUrl().toURI();
            log.debug("Fetching command via HTTP from URL: {}", uri.toString());
            httpPost = new HttpPost(uri);
        } catch (URISyntaxException e) {
            log.error("Error building HTTP URI for fetching command!", e);
            return false;
        }

        setAgentMetaHeaders(httpPost);

        StringEntity payloadEntity = new StringEntity(objectMapper.writeValueAsString(payload));
        httpPost.setEntity(payloadEntity);
        httpPost.setHeader("Content-Type", "application/json");
        boolean isError = true;
        try {
            HttpResponse response = createHttpClient(timeout).execute(httpPost);
            processHttpResponse(response);
            isError = false;
        } catch (Exception e) {
            log.error("An error occurred while fetching a new command: " + e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }

        return isError;
    }

    /**
     * Injects all the agent's meta information headers, which should be send when fetching a new configuration,
     * into the given request request.
     *
     * @param httpPost the request to inject the meta information headers
     */
    private void setAgentMetaHeaders(HttpPost httpPost) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        httpPost.setHeader(META_HEADER_PREFIX + "AGENT-ID", runtime.getName());
        httpPost.setHeader(META_HEADER_PREFIX + "AGENT-VERSION", AgentManager.getAgentVersion());
        httpPost.setHeader(META_HEADER_PREFIX + "JAVA-VERSION", System.getProperty("java.version"));
        httpPost.setHeader(META_HEADER_PREFIX + "VM-NAME", runtime.getVmName());
        httpPost.setHeader(META_HEADER_PREFIX + "VM-VENDOR", runtime.getVmVendor());
        httpPost.setHeader(META_HEADER_PREFIX + "START-TIME", String.valueOf(runtime.getStartTime()));
    }

    /**
     * Takes a HttpResponse as a parameter and processes it. If the response contains an instance of {@link Command}, the
     * the command is executed and the respective {@link CommandResponse} is returned.
     *
     * @param response The response to be send as payload.
     */
    private void processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            HttpEntity responseEntity = response.getEntity();
            InputStream content = responseEntity.getContent();
            Command command = null;
            try {
                command = objectMapper.readValue(content,  Command.class);
            }catch(MismatchedInputException e) {
                log.info("Empty command received!");
            }

            if (command == null) {
                if (repeat && repeatCounter < maxRepeats) {
                    repeatCounter++;
                    fetchCommand(null, discoveryTimeoutDuration);
                } else {
                    repeat = false;
                }
            } else {
                CommandResponse commandResult = commandDelegator.delegate(command);

                repeatCounter = 0;
                repeat = true;
                fetchCommand(commandResult, discoveryTimeoutDuration);
            }

        } else {
            throw new IOException("Server returned an unexpected status code: " + statusCode);
        }
    }

}
