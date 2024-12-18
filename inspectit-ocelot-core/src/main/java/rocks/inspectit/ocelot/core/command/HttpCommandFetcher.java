package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.command.http.CommandHttpClientHolder;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The prefix which is used for the meta information HTTP headers.
     */
    private static final String META_HEADER_PREFIX = "X-OCELOT-";

    /**
     * The holder of the HTTP clients for agent commands.
     */
    private final CommandHttpClientHolder clientHolder = new CommandHttpClientHolder();

    /**
     * The URI for fetching commands.
     */
    @Setter
    private URI commandUri;

    /**
     * Fetches a {@link Command} by sending the given {@link CommandResponse} as payload and uses the given timeout-int as timeout.
     *
     * @param commandResponse The payload to be sent.
     * @param waitForCommand  Whether the agent should wait for commands.
     *
     * @return returns An HttpResponse or <code>null</code> if any error occurred before sending the request.
     *
     * @throws IOException if communication with the server was not successful.
     */
    public HttpResponse fetchCommand(CommandResponse commandResponse, boolean waitForCommand) throws IOException {
        HttpPost httpPost;
        try {
            URIBuilder uriBuilder = new URIBuilder(commandUri);
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

        HttpClient httpClient = getHttpClient(waitForCommand);

        try {
            return httpClient.execute(httpPost);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Injects all the agent's meta information headers, which should be sent when fetching a command,
     * into the given request.
     *
     * @param httpPost the request to inject the meta information headers
     */
    private void setAgentMetaHeaders(HttpPost httpPost) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        httpPost.setHeader(META_HEADER_PREFIX + "AGENT-ID", runtime.getName());
    }

    /**
     * Returns the {@link HttpClient} which is used for fetching commands.
     *
     * @param liveClient true, if live-mode is active
     * @return A {@link HttpClient} instance.
     */
    private HttpClient getHttpClient(boolean liveClient) throws IOException {
        AgentCommandSettings currentSettings = environment.getCurrentConfig().getAgentCommands();

        if(liveClient) return clientHolder.getLiveHttpClient(currentSettings);
        else return clientHolder.getDiscoveryHttpClient(currentSettings);
    }
}
