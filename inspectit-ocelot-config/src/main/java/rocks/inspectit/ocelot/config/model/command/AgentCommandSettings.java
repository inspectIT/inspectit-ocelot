package rocks.inspectit.ocelot.config.model.command;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AgentCommandSettings {

    /**
     * Whether commands are enabled or not.
     */
    private boolean enabled = false;

    /**
     * The URL for getting agent commands over grpc, e.g. "localhost:9090".
     */
    private String url;

    /**
     * Whether the agent commands URL should be derived from the HTTP configuration URL.
     */
    private boolean deriveFromHttpConfigUrl = false;

    /**
     * Port which is used for the agent command URL in case it is derived from the HTTP configuration URL
     */
    private Integer agentCommandPort;

    /**
     * Maximum size for inbound grpc messages, i.e. commands from config-server, in MiB.
     * Commands probably will never exceed grpc's default of 4MiB that is also set as default here,
     * but if your commands do you can configure it.
     */
    private Integer maxInboundMessageSize = 4;

    /**
     * Time after which the backoff between retries to re-establish the grpc connection between agent and config-server
     * is reset to the lowest value.
     */
    private int backoffResetTime = 60;

    /**
     * How often the backoff between retries to re-establish the grpc connection between agent and config-server is increased for the next retry.
     * Backoff is calculated as 2 to the power of how often the backoff has been increased, so a value of 5 means that the max backoff is 32 seconds.
     * <p>
     * This setting only sets a maximum for the backoff between retries, it does not affect the number of retries,
     * the service will always continue to try reconnecting on errors unless disabled.
     */
    private int maxBackoffIncreases = 5;

}
