package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentCommandSettings {

    /**
     * Timeout how long a command will wait for a response from the agent before it will be removed.
     */
    @Builder.Default
    private Duration responseTimeout = Duration.ofSeconds(30);

    /**
     * Maximum size for inbound grpc messages, i.e. responses from agents, in MiB.
     * Default is 4MiB which is also grpc's default.
     */
    @Builder.Default
    private int maxInboundMessageSize = 4;
}
