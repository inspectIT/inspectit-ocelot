package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.AgentCommandsGrpc;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@GrpcService
@Slf4j
public class CommandsGrpcService extends AgentCommandsGrpc.AgentCommandsImplBase {

    @Autowired
    private InspectitServerSettings configuration;

    @Autowired
    private AgentCallbackManager callbackManager;

    /**
     * Keys are agent-ids and values the corresponding StreamObserver that can be used to send commands to that agent.
     */
    BiMap<String, StreamObserver<Command>> agentConnections = Maps.synchronizedBiMap(HashBiMap.create());

    public DeferredResult<ResponseEntity<?>> dispatchCommand(String agentId, Command command) throws ExecutionException {

        // TODO: 03.03.2022 Move this comment into Reviewable comment
        // Build Response here, since it is the same in each handler
        Duration responseTimeout = configuration.getAgentCommand().getResponseTimeout();
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(responseTimeout.toMillis());
        deferredResult.onTimeout(() -> ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build());

        callbackManager.addCommandCallback(UUID.fromString(command.getCommandId()), deferredResult);

        // Get connection to agent
        StreamObserver<Command> commandObserver = agentConnections.get(agentId);
        if (commandObserver != null) {
            // Send command to agent
            log.info("Sending command '{}' to agent '{}'.", command.getCommandId(), agentId);
            commandObserver.onNext(command);
        } else {
            // If no connection to agent exists, return error.
            log.error("Command for agent '{}' was requested, but can not find a connection for that agent.", agentId);
            deferredResult.setErrorResult(new RuntimeException(String.format("Can not find a connection to any agent with ID '%s'.", agentId)));
        }
        return deferredResult;
    }

    @Override
    public StreamObserver<CommandResponse> askForCommands(StreamObserver<Command> commandsObserver) {
        return new StreamObserver<CommandResponse>() {

            @Override
            public void onNext(CommandResponse commandResponse) {
                if (commandResponse.hasFirst()) {
                    String agentId = commandResponse.getFirst().getAgentId();
                    log.info("New agent '{}' connected itself to config-server.", agentId);
                    agentConnections.put(agentId, commandsObserver);
                } else {
                    String agentId = agentConnections.inverse().get(commandsObserver);
                    log.info("Agent '{}' answered to '{}'.", agentId, commandResponse.getCommandId());
                    callbackManager.handleCommandResponse(UUID.fromString(commandResponse.getCommandId()), commandResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                String agentId = agentConnections.inverse().get(commandsObserver);
                log.error("Encountered error in exchangeInformation ending the stream connection with agent {}. {}", agentId, t.toString());
                agentConnections.remove(agentId);
            }

            @Override
            public void onCompleted() {
                String agentId = agentConnections.inverse().get(commandsObserver);
                log.info("Agent '{}' ended the stream connection.", agentId);
                agentConnections.remove(agentId);
                commandsObserver.onCompleted();
            }
        };
    }

    @Bean
    public GrpcServerConfigurer keepAliveServerConfigurer() {
        return serverBuilder -> {
            if (serverBuilder instanceof NettyServerBuilder) {
                ((NettyServerBuilder) serverBuilder).maxInboundMessageSize(configuration.getAgentCommand()
                        .getMaxInboundMessageSize() * 1024 * 1024);
            }
        };
    }
}