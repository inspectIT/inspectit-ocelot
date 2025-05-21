package rocks.inspectit.ocelot.core.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.net.URIBuilder;
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
    public ClassicHttpResponse fetchCommand(CommandResponse commandResponse, boolean waitForCommand) throws IOException {
        ClassicHttpRequest httpPost;
        try {
            URIBuilder uriBuilder = new URIBuilder(commandUri);
            if (waitForCommand) {
                uriBuilder.addParameter("wait-for-command", "true");
            }
            URI uri = uriBuilder.build();

            log.debug("Fetching command via HTTP from URL: {}", uri.toString());
            httpPost = ClassicRequestBuilder.post().setUri(uri).build();
        } catch (URISyntaxException e) {
            log.error("Error building HTTP URI for fetching command!", e);
            return null;
        }

        // add response if existing
        if (commandResponse != null) {
            try {
                StringEntity payloadEntity = new StringEntity(objectMapper.writeValueAsString(commandResponse));
                httpPost.setEntity(payloadEntity);
            } catch (JsonProcessingException e) {
                log.error("Error serializing the command response!", e);
                return null;
            }
        }

        // set headers
        setAgentMetaHeaders(httpPost);
        httpPost.setHeader("Content-Type", "application/json");

        CloseableHttpClient httpClient = getHttpClient(waitForCommand);

        // we could try to use a ResponseHandler here
        return httpClient.execute(httpPost);
    }

    /**
     * Injects all the agent's meta information headers, which should be sent when fetching a command,
     * into the given request.
     *
     * @param httpPost the request to inject the meta information headers
     */
    private void setAgentMetaHeaders(ClassicHttpRequest httpPost) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        httpPost.setHeader(META_HEADER_PREFIX + "AGENT-ID", runtime.getName());
    }

    /**
     * Returns the {@link CloseableHttpClient} which is used for fetching commands.
     *
     * @param liveClient true, if live-mode is active
     * @return A {@link CloseableHttpClient} instance.
     */
    private CloseableHttpClient getHttpClient(boolean liveClient) throws IOException {
        AgentCommandSettings currentSettings = environment.getCurrentConfig().getAgentCommands();

        if(liveClient) return clientHolder.getLiveHttpClient(currentSettings);
        else return clientHolder.getDiscoveryHttpClient(currentSettings);
    }
}
