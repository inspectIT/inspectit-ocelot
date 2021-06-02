package rocks.inspectit.ocelot.commons.models.command.response.impl;

import lombok.*;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.UUID;

/**
 * Represents a response to the {@link rocks.inspectit.ocelot.commons.models.command.impl.PingCommand}.
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingResponse extends CommandResponse {

    @Builder
    public PingResponse(UUID commandId) {
        super(commandId);
    }
}
