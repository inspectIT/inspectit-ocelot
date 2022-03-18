package rocks.inspectit.ocelot.core.command;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.grpc.Command;

import java.io.File;
import java.io.IOException;
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

    ManagedChannel channel;

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

        // only actually enable the feature if the url is based on the HTTP config URL OR the url is specified directly
        if (settings.isDeriveHostFromHttpConfigUrl()) {
            return true;
        } else {
            return StringUtils.isNotEmpty(settings.getHost());
        }
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            log.info("Starting agent command service.");

            AgentCommandSettings settings = configuration.getAgentCommands();
            String commandsHost = getCommandHost(configuration);
            int commandsPort = configuration.getAgentCommands().getPort();

            channel = getChannel(settings, commandsHost, commandsPort);

            client = new AgentCommandClient(channel);

            log.info("Connecting to Configserver over grpc for agent commands over URL '{}:{}' with agent ID '{}'", commandsHost, commandsPort, client.getAgentId());

            return client.startAskForCommandsConnection(settings, this);

        } catch (Exception e) {
            log.error("Could not enable the agent command service.", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command service.");

        if (client != null) {
            client.shutdown();
            client = null;
        }

        if (channel != null) {
            channel.shutdown();
            channel = null;
        }

        return true;
    }

    @VisibleForTesting
    String getCommandHost(InspectitConfig configuration) {
        AgentCommandSettings settings = configuration.getAgentCommands();

        if (settings.isDeriveHostFromHttpConfigUrl()) {
            URL url = configuration.getConfig().getHttp().getUrl();
            if (url == null) {
                throw new IllegalStateException("The URL cannot be derived from the HTTP configuration URL because it is null.");
            }
            return url.getHost();
        } else {
            return settings.getHost();
        }
    }

    @VisibleForTesting
    ManagedChannel getChannel(AgentCommandSettings settings, String host, int port) throws IOException {

        ManagedChannel channel;

        if (settings.isUseTls()) {
            TlsChannelCredentials.Builder credsBuilder = TlsChannelCredentials.newBuilder();

            String clientCertChainFilePath = settings.getClientCertChainFilePath();
            String clientPrivateKeyFilePath = settings.getClientPrivateKeyFilePath();
            if (StringUtils.isNotEmpty(clientCertChainFilePath) && StringUtils.isNotEmpty(clientPrivateKeyFilePath)) {
                log.info("Setting up client certificate with clientCertChainFilePath='{}' and clientPrivateKeyFilePath='{}' for mutual authentication.", clientCertChainFilePath, clientPrivateKeyFilePath);
                credsBuilder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
            } else if (StringUtils.isNotEmpty(clientCertChainFilePath) ^ StringUtils.isNotEmpty(clientPrivateKeyFilePath)) {
                throw new IllegalStateException(String.format("Only one of clientCertChainFilePath='%s' and clientPrivateKeyFilePath='%s' is set, but either both need to be set or neither.", clientCertChainFilePath, clientPrivateKeyFilePath));
            } else {
                log.info("Using TLS without mutual authentication.");
            }

            String trustCertCollectionFilePath = settings.getTrustCertCollectionFilePath();
            if (StringUtils.isNotEmpty(trustCertCollectionFilePath)) {
                credsBuilder.trustManager(new File(trustCertCollectionFilePath));
                log.info("Adding trustCertCollection='{}' for grpc connection.", trustCertCollectionFilePath);
            }

            channel = Grpc.newChannelBuilderForAddress(host, port, credsBuilder.build())
                    .maxInboundMessageSize(settings.getMaxInboundMessageSize() * 1024 * 1024)
                    .overrideAuthority("foo.test.google.com.au")
                    .build();
        } else {
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        }

        return channel;
    }
}
