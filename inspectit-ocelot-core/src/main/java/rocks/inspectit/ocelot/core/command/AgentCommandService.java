package rocks.inspectit.ocelot.core.command;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.grpc.AgentCommandsGrpc;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.FirstResponse;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    // I think volatile is needed because of the possibility of retrying connecting and disabling at the same time
    volatile private StreamObserver<CommandResponse> commandResponseObserver = null;

    private long lastRetryAttempt;

    ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);

    private int retriesAttempted = 0;

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

            AgentCommandsGrpc.AgentCommandsStub asyncStub = AgentCommandsGrpc.newStub(channel);

            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            String agentId = runtime.getName();

            log.info("Connecting to Configserver over grpc for agent commands over URL '{}' with agent ID '{}'", commandsUrl, agentId);

            return startAskForCommandsConnection(configuration.getAgentCommands(), asyncStub, agentId);

        } catch (Exception e) {
            log.error("Could not enable the agent command service.", e);
            return false;
        }
    }

    private boolean startAskForCommandsConnection(AgentCommandSettings settings, AgentCommandsGrpc.AgentCommandsStub asyncStub, String agentId) {
        try {

            StreamObserver<Command> commandObserver = new StreamObserver<Command>() {
                @Override
                public void onNext(Command command) {
                    try {
                        log.info("Received command with id '{}' from config-server.", command.getCommandId());
                        commandResponseObserver.onNext(commandDelegator.delegate(command));
                        log.info("Answered to command with id '{}'.", command.getCommandId());
                    } catch (Exception exception) {
                        // TODO: 03.03.2022 Send an answer with Exception Message?
                        log.error("Exception during agent command execution.", exception);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Encountered error in exchangeInformation ending the stream connection with config-Server. {}", t.toString());

                    long currentTime = System.nanoTime();

                    if (retriesAttempted > 0) {
                        if ((currentTime - lastRetryAttempt) > (Math.pow(10, 9) * settings.getBackoffResetTime())) {
                            retriesAttempted = 0;
                        }
                    }

                    ScheduledFuture<?> restartFuture = reconnectExecutor.schedule(() -> {
                        boolean success = startAskForCommandsConnection(settings, asyncStub, agentId);
                        if (success) {
                            log.info("Successfully restarted connection after error.");
                        } else {
                            disable();
                            log.info("Could not restart connection after error.");
                        }
                    }, (long) Math.pow(2, retriesAttempted), TimeUnit.SECONDS);

                    if (retriesAttempted < settings.getMaxBackoffIncreases()) {
                        retriesAttempted++;
                    }

                    lastRetryAttempt = currentTime;
                }

                @Override
                public void onCompleted() {
                    log.info("Received completion acknowledgement from config-Server.");
                    commandResponseObserver = null;
                }

            };

            commandResponseObserver = asyncStub.askForCommands(commandObserver);

            commandResponseObserver.onNext(CommandResponse.newBuilder()
                    .setFirst(FirstResponse.newBuilder().setAgentId(agentId))
                    .buildPartial());
        } catch (Exception e) {
            log.error("Could not start askForCommands-connection.", e);
            commandResponseObserver = null;
            return false;
        }

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command service.");

        reconnectExecutor.shutdownNow();

        if (commandResponseObserver != null) {
            try {
                commandResponseObserver.onCompleted();
            } catch (NullPointerException e) {
                log.debug("Encountered null exception while disabling agent command service, probably because a thread trying to restart the connection was running and modifying commandResponseObserver at the same time.", e);
            }
            commandResponseObserver = null;
        }
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
