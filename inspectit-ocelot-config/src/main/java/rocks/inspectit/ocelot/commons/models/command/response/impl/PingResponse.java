package rocks.inspectit.ocelot.commons.models.command.response.impl;

import lombok.*;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.UUID;

/**
 * Represents a Ping-Response. An agent returns a Ping-Response with alive set to true if the agent is available.
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingResponse extends CommandResponse {

    @Builder
    public PingResponse(UUID commandId) {
        super(commandId);
    }
}
