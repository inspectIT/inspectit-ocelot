package rocks.inspectit.ocelot.commons.models.command.response.impl;

import lombok.*;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.UUID;

/**
 * Represents a Ping-Response. An agent returns a Ping-Response with alive set to true if the agent is available.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingResponse extends CommandResponse {

    boolean alive;

    @Builder
    public PingResponse(UUID commandId, boolean alive) {
        super(commandId);
        this.alive = alive;
    }
}
