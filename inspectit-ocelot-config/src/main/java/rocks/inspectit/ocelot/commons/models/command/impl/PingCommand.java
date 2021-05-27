package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import rocks.inspectit.ocelot.commons.models.command.Command;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingCommand extends Command {

}