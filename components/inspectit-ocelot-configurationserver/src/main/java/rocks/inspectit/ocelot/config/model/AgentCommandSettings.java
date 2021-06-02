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
     * Timeout for agent commands. After this duration, they will be removed if not fetched.
     */
    @Builder.Default
    private Duration commandTimeout = Duration.ofMinutes(2);

    /**
     * Timeout how long a command will wait for a response from the agent before it will be removed.
     */
    @Builder.Default
    private Duration responseTimeout = Duration.ofSeconds(30);

    /**
     * The size of each agents' command queue.
     */
    @Builder.Default
    private int commandQueueSize = 100;

    /**
     * The max. time an agent is allowed to wait for a new command.
     */
    @Builder.Default
    private Duration agentPollingTimeout = Duration.ofSeconds(30);
}
