package rocks.inspectit.ocelot.core.command;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.grpc.Command;

import java.net.URL;

/**
 * Service responsible for fetching agent commands.
 */
@Service
@Slf4j
@Getter
public class AgentCommandService extends DynamicallyActivatableService {

    /**
     * Used to delegate received {@link Command} objects to their respective implementation of {@link rocks.inspectit.ocelot.core.command.handler.CommandExecutor}.
     */
    @Autowired
    private CommandDelegator commandDelegator;

    AgentCommandClient client;

    public AgentCommandService() {
        super("agentCommands");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        AgentCommandSettings settings = configuration.getAgentCommands();
        // the feature has to be enabled
        if (!settings.isEnabled()) {
            return false;
        }

        // enable the feature if the url is based on the HTTP config URL OR the url is specified directly
        if (settings.isDeriveFromHttpConfigUrl()) {
            return true;
        } else {
            return StringUtils.isNotEmpty(settings.getUrl());
        }
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            String commandsUrl = getCommandUrl(configuration);
            Integer grpcMaxSize = configuration.getAgentCommands().getMaxInboundMessageSize();

            Channel channel = ManagedChannelBuilder.forTarget(commandsUrl)
                    .maxInboundMessageSize(grpcMaxSize * 1024 * 1024)
                    .usePlaintext()
                    .build();

            client = new AgentCommandClient(channel);

            log.info("Connecting to Configserver over grpc for agent commands over URL '{}' with agent ID '{}'", commandsUrl, client.getAgentId());

            return client.startAskForCommandsConnection(configuration.getAgentCommands(), this);

        } catch (Exception e) {
            log.error("Could not enable the agent command service.", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command service.");

        client.shutdown();
        client = null;

        return true;
    }

    @VisibleForTesting
    String getCommandUrl(InspectitConfig configuration) {
        AgentCommandSettings settings = configuration.getAgentCommands();

        if (settings.isDeriveFromHttpConfigUrl()) {
            URL url = configuration.getConfig().getHttp().getUrl();
            Integer port = settings.getAgentCommandPort();
            if (url == null) {
                throw new IllegalStateException("The URL cannot be derived from the HTTP configuration URL because it is null.");
            } else if (port == null) {
                throw new IllegalStateException("The URL cannot be derived from the HTTP configuration URL because the agentCommandPort is null.");
            }
            return String.format("%s:%s", url.getHost(), settings.getAgentCommandPort());
        } else {
            return settings.getUrl();
        }
    }
}
