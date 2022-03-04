package rocks.inspectit.ocelot.core.command;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
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

/**
 * Service responsible for fetching agent commands.
 */
@Service
@Slf4j
public class AgentCommandService extends DynamicallyActivatableService {

    /**
     * Used to delegate recieved {@link Command} objects to their respective implementation of {@link rocks.inspectit.ocelot.core.command.handler.CommandExecutor}.
     */
    @Autowired
    private CommandDelegator commandDelegator;

    private StreamObserver<CommandResponse> commandResponseObserver = null;

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
            // TODO: 09.03.2022 derive from http configuration address 
            String commandsAddress = configuration.getAgentCommands().getUrl();
            Integer grpcMaxSize = configuration.getAgentCommands().getMaxInboundMessageSize();

            Channel channel = ManagedChannelBuilder.forTarget(commandsAddress)
                    .maxInboundMessageSize(grpcMaxSize * 1024 * 1024)
                    .usePlaintext()
                    .build();

            AgentCommandsGrpc.AgentCommandsStub asyncStub = AgentCommandsGrpc.newStub(channel);

            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            String agentId = runtime.getName();

            log.info("Connecting to Configserver over grpc for agent commands over URL '{}' with agent ID '{}'", commandsAddress, agentId);

            commandResponseObserver = asyncStub.askForCommands(new StreamObserver<Command>() {
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
                    commandResponseObserver = null;
                    // TODO: 04.03.2022 try to restart connection, disable service if not possible
                }

                @Override
                public void onCompleted() {
                    log.info("Received completion acknowledgement from config-Server.");
                    commandResponseObserver = null;
                }

            });

            commandResponseObserver.onNext(CommandResponse.newBuilder()
                    .setFirst(FirstResponse.newBuilder().setAgentId(agentId))
                    .buildPartial());
        } catch (Exception e) {
            log.error("Could not enable the agent command service.", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command service.");

        if (commandResponseObserver != null) {
            commandResponseObserver.onCompleted();
        }
        return true;
    }
}
