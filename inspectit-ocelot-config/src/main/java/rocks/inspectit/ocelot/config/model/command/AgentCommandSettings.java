package rocks.inspectit.ocelot.config.model.command;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.time.Duration;

@Data
@NoArgsConstructor
public class AgentCommandSettings {

    /**
     * Whether commands are enabled or not.
     */
    private boolean enabled = false;

    /**
     * The URL for fetching agent commands.
     */
    private URL url;

    /**
     * The URL for getting agent commands over grpc.
     */
    private String grpcUrl;

    /**
     * Whether the agent commands URL should be derived from the HTTP configuration URL.
     */
    private boolean deriveFromHttpConfigUrl = false;

    /**
     * Path which is used for the agent command URL in case it is derived from the HTTP configuration URL
     */
    private String agentCommandPath;

    /**
     * The timeout duration used for requests  when the agent is in discovery mode. Defining how long the agent will wait for
     * new commands.
     */
    private Duration liveSocketTimeout;

    /**
     * The timeout duration used for requests when the agent is in normal mode.
     */
    private Duration socketTimeout;

    /**
     * The used interval for polling commands.
     */
    private Duration pollingInterval;

    /**
     * How long the agent will staying in the live mode, before falling back to the normal mode.
     */
    private Duration liveModeDuration;
}
