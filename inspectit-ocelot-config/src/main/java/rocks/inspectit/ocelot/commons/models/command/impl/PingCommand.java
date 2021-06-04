package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import rocks.inspectit.ocelot.commons.models.command.Command;

/**
 * Represents a Ping-Command. Ping commands are used to check if the receiving agent does exist.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingCommand extends Command {

}