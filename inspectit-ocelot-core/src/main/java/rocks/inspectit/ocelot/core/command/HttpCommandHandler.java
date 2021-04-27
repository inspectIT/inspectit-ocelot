package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.commons.models.AgentCommand;
import rocks.inspectit.ocelot.commons.models.AgentCommandType;
import rocks.inspectit.ocelot.commons.models.AgentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class HttpCommandHandler {

    private final ObjectMapper objectMapper;

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
    private final int DISCOVERY_TIMEOUT_DURATION = 100000;

    private int repeatCounter = 0;

    /**
     * Constructor.
     *
     * @param currentSettings the settings used to fetch the configuration
     */
    public HttpCommandHandler(HttpConfigSettings currentSettings) {
        objectMapper = new ObjectMapper();
        this.currentSettings = currentSettings;
    }

    /**
     * Creates the {@link HttpClient} which is used for fetching commands.
     *
     * @return A new {@link HttpClient} instance.
     */
    private HttpClient createHttpClient(int timeOut) {
        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (currentSettings.getConnectionTimeout() != null) {
            configBuilder = configBuilder.setConnectTimeout(timeOut);
        }
        if (currentSettings.getSocketTimeout() != null) {
            int socketTimeout = (int) currentSettings.getSocketTimeout().toMillis();
            configBuilder = configBuilder.setSocketTimeout(socketTimeout);
        }

        RequestConfig config = configBuilder.build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    /**
     * Fetches an AgentCommand by sending an empty payload. Uses the DEFAULT_TIMEOUT_DURATION as timeout value.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    public boolean fetchCommand() throws UnsupportedEncodingException, JsonProcessingException {
        return fetchCommand(AgentResponse.getEmptyResponse());
    }

    /**
     * Fetches an AgentCommand by sending the given parameter as payload. Uses the DEFAULT_TIMEOUT_DURATION as timeout value.
     *
     * @param payload The payload to be send.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    private boolean fetchCommand(AgentResponse payload) throws UnsupportedEncodingException, JsonProcessingException {
        return fetchCommand(payload, DEFAULT_TIMEOUT_DURATION);
    }

    /**
     * Fetches an AgentCommand by sending the given parameter as payload and uses the given int as timeout.
     *
     * @param payload The payload to be send.
     * @param timeout The timeout to be used for the request.
     *
     * @return returns false if the request was send and true if it could not be send.
     */
    private boolean fetchCommand(AgentResponse payload, int timeout) throws JsonProcessingException, UnsupportedEncodingException {
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
        Gson gson = new Gson();

        StringEntity payloadEntity = new StringEntity(gson.toJson(payload));
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
     * @param httpPost the request to inject the meat information headers
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
     * Takes a HttpResponse as a parameter and processes it. If the response contains a AgentCommand, the
     * the command is executed. The return value is
     *
     * @param response The response to be send as payload.
     */
    private void processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            try {
                HttpEntity responseContent = response.getEntity();
                InputStream content = responseContent.getContent();
                AgentCommand agentCommand = objectMapper.readValue(content, AgentCommand.class);

                if (AgentCommand.getEmptyCommand().equals(agentCommand)) {
                    log.info("Empty command received!");
                    if (repeatCounter < 3) {
                        repeatCounter++;
                        fetchCommand(AgentResponse.getEmptyResponse(), DISCOVERY_TIMEOUT_DURATION);
                    }
                } else {
                    Object commandResult = executeCommand(agentCommand);

                    AgentResponse agentResponse = new AgentResponse(agentCommand.getCommandId(), commandResult);
                    repeatCounter = 0;
                    fetchCommand(agentResponse, DISCOVERY_TIMEOUT_DURATION);
                }
            } catch (IOException | UnsupportedOperationException e) {
                log.error("An error occurred while fetching a new command: " + e.getMessage());
            }

        } else {
            throw new IOException("Server returned an unexpected status code: " + statusCode);
        }
    }

    /**
     * Executes a given command and returns the commands return value.
     *
     * @param agentCommand The command to be executed.
     *
     * @return The return value of the command.
     */
    private Object executeCommand(AgentCommand agentCommand) {
        if (agentCommand.getCommandType() == AgentCommandType.GET_HEALTH) {
            return "OK";
        }
        return null;

    }

}
