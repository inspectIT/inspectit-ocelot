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
     * The URL for getting agent commands over grpc.
     */
    private String url;

    /**
     * Whether the agent commands URL should be derived from the HTTP configuration URL.
     */
    private boolean deriveFromHttpConfigUrl = false;

    /**
     * Path which is used for the agent command URL in case it is derived from the HTTP configuration URL
     */
    private int agentCommandPort;

    /**
     * Maximum size for inbound grpc messages, i.e. commands from config-server, in MiB.
     * Commands probably will never exceed grpc's default of 4MiB that is also set as default here,
     * but if your commands do you can configure it.
     */
    private Integer maxInboundMessageSize = 4;
}
