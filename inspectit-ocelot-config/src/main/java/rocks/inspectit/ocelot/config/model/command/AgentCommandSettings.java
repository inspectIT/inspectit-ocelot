package rocks.inspectit.ocelot.config.model.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
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
     * The TTL - the time to keep an HTTP connection alive
     */
    private Duration timeToLive;

    /**
     * The used interval for polling commands.
     */
    private Duration pollingInterval;

    /**
     * The task timeout to use - the time to run one agent command task.
     */
    private Duration taskTimeout;

    /**
     * How long the agent will stay in the live mode, before falling back to the normal mode.
     */
    private Duration liveModeDuration;

    /**
     * Settings how retries are handled regarding fetching an agent command.
     */
    @Valid
    private RetrySettings retry;

    @AssertFalse(message = "The specified time values should not be negative!")
    public boolean isNegativeTimeout() {
        boolean negativeLiveReadTimeout = liveSocketTimeout != null && liveSocketTimeout.isNegative();
        boolean negativeReadTimeout = socketTimeout != null && socketTimeout.isNegative();
        boolean negativeTTL = timeToLive != null && timeToLive.isNegative();
        return negativeLiveReadTimeout || negativeReadTimeout || negativeTTL;
    }
}
