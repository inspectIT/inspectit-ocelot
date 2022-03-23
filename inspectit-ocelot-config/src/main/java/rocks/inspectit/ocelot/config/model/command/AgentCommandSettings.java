package rocks.inspectit.ocelot.config.model.command;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
public class AgentCommandSettings {

    /**
     * Whether commands are enabled or not.
     */
    private boolean enabled = false;

    /**
     * Whether the agent commands URL should be derived from the HTTP configuration URL. This has priority over {@link #host}.
     */
    private boolean deriveHostFromHttpConfigUrl = false;

    /**
     * The hostname for getting agent commands over grpc, e.g. "localhost".
     */
    private String host;

    /**
     * Port the agent will use to connect to the config-server over grpc, should correspond to grpc.server.port in config-server's properties
     */
    private int port;

    /**
     * Whether agent should use TLS to connect to config-server over grpc.
     */
    private boolean useTls = true;

    /**
     * Path to a collection of trusted certificates, needed for TLS for agent commands if server's certificate does not chain to a standard root.
     */
    private String trustCertCollectionFilePath;

    /**
     * If the server's certificate is not valid for the hostname the agent uses to connect itself,
     * this option can be used to add that name to fix the problem
     */
    private String authorityOverride;

    /**
     * Path to certificate file for client, i.e. agent, needed only for mutual authentication.
     * If set, {@link #clientPrivateKeyFilePath} also needs to be set.
     */
    private String clientCertChainFilePath;

    /**
     * Path to private key file for client, i.e. agent, needed only for mutual authentication.
     * If set, {@link #clientCertChainFilePath} also needs to be set.
     */
    private String clientPrivateKeyFilePath;

    /**
     * Maximum size for inbound grpc messages, i.e. commands from config-server, in MiB.
     * Commands probably will never exceed grpc's default of 4MiB that is also set as default here,
     * but if your commands do you can configure it.
     */
    private int maxInboundMessageSize = 4;

    /**
     * Time after which the backoff between retries to re-establish the grpc connection between agent and config-server
     * is reset to the lowest value.
     */
    private Duration backoffResetTime = Duration.ofSeconds(60);

    /**
     * How often the backoff between retries to re-establish the grpc connection between agent and config-server is increased for the next retry.
     * Backoff is calculated as 2 to the power of how often the backoff has been increased, so a value of 5 means that the max backoff is 32 seconds.
     * <p>
     * This setting only sets a maximum for the backoff between retries, it does not affect the number of retries,
     * the service will always continue to try reconnecting on errors unless disabled.
     */
    private int maxBackoffIncreases = 5;

}
