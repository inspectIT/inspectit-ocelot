package rocks.inspectit.ocelot.commons.models.command.response.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PingResponse extends CommandResponse {

    boolean alive;

    @Builder
    public PingResponse(UUID commandId, boolean alive) {
        super(commandId);
        this.alive = alive;
    }
}
