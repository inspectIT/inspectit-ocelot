package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import rocks.inspectit.ocelot.commons.models.command.Command;

/**
 * Represents a Ping-Command. Ping commands are used by the health-check endpoint of the config-server to check if the
 * recieving agent is alive.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingCommand extends Command {

}