package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class HttpCommandFetcher {

    @Autowired
    protected InspectitEnvironment environment;

    /**
     * Object mapper for serializing command responses.
     */
    private final ObjectMapper objectMapper = new ObjectMapper().enableDefaultTyping();

    /**
     * The prefix which is used for the meta information HTTP headers.
     */
    private static final String META_HEADER_PREFIX = "X-OCELOT-";

    /**
     * Http client used in the normal mode.
     */
    private HttpClient normalHttpClient;

    /**
     * Http client used in the live mode (longer timeouts).
     */
    private HttpClient liveHttpClient;

    /**
     * Returns the {@link HttpClient} which is used for fetching commands.
     *
     * @return A new {@link HttpClient} instance.
     */
    private HttpClient getHttpClient(boolean liveClient) {
        if (normalHttpClient == null || liveHttpClient == null) {
            updateHttpClients();
        }

        return liveClient ? liveHttpClient : normalHttpClient;
    }

    /**
     * Updating the http clients.
     */
    private void updateHttpClients() {
        AgentCommandSettings settings = environment.getCurrentConfig().getAgentCommands();
        int timeout = (int) settings.getSocketTimeout().toMillis();
        int liveTimeout = (int) settings.getLiveSocketTimeout().toMillis();

        RequestConfig normalConfig = RequestConfig.custom().setSocketTimeout(timeout).build();

        RequestConfig liveConfig = RequestConfig.custom().setSocketTimeout(liveTimeout).build();

        normalHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(normalConfig).build();
        liveHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(liveConfig).build();
    }

    /**
     * Fetches a {@link Command} by sending the given {@link CommandResponse} as payload and uses the given timeout-int as timeout.
     *
     * @param commandResponse The payload to be send.
     * @param waitForCommand  Whether the agent should wait for commands.
     *
     * @return returns null if any error occurred before/while sending the request.
     */
    public HttpResponse fetchCommand(CommandResponse commandResponse, boolean waitForCommand)  {
        HttpPost httpPost;
        try {
            AgentCommandSettings settings = environment.getCurrentConfig().getAgentCommands();

            URIBuilder uriBuilder = new URIBuilder(settings.getUrl().toURI());
            if (waitForCommand) {
                uriBuilder.addParameter("wait-for-command", "true");
            }
            URI uri = uriBuilder.build();

            log.debug("Fetching command via HTTP from URL: {}", uri.toString());
            httpPost = new HttpPost(uri);
        } catch (URISyntaxException e) {
            log.error("Error building HTTP URI for fetching command!", e);
            return null;
        }

        // add response if existing
        if (commandResponse != null) {
            try {
                StringEntity payloadEntity = new StringEntity(objectMapper.writeValueAsString(commandResponse));
                httpPost.setEntity(payloadEntity);
            } catch (JsonProcessingException | UnsupportedEncodingException e) {
                log.error("Error serializing the command response!", e);
                return null;
            }
        }

        // set headers
        setAgentMetaHeaders(httpPost);
        httpPost.setHeader("Content-Type", "application/json");

        try {
            return getHttpClient(waitForCommand).execute(httpPost);
        } catch (Exception e) {
            log.error("An error occurred while fetching a new command: " + e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }

        return null;
    }

    /**
     * Injects all the agent's meta information headers, which should be send when fetching a command,
     * into the given request request.
     *
     * @param httpPost the request to inject the meta information headers
     */
    private void setAgentMetaHeaders(HttpPost httpPost) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        httpPost.setHeader(META_HEADER_PREFIX + "AGENT-ID", runtime.getName());
    }
}
