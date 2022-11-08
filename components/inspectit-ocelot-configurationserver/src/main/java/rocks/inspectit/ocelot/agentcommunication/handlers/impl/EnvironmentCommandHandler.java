package rocks.inspectit.ocelot.agentcommunication.handlers.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import rocks.inspectit.ocelot.commons.models.command.impl.EnvironmentCommand;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.time.Duration;

/**
 * Handler for the Agent Environment command.
 */
@Slf4j
@Component
public class EnvironmentCommandHandler  {//implements CommandHandler {

    @Autowired
    private InspectitServerSettings configuration;

    /**
     * Checks if the given {@link Command} is an instance of {@link EnvironmentCommand}.
     *
     * @param command The command which should be checked.
     * @return True if the given command is an instance of {@link EnvironmentCommand}.
     */


}
