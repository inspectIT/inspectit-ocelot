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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.AgentCommandsGrpc;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@GrpcService
@Slf4j
public class CommandsService extends AgentCommandsGrpc.AgentCommandsImplBase {

    @Autowired
    private InspectitServerSettings configuration;

    @Autowired
    private AgentCallbackManager callbackManager;

    @Value("${grpc.server.security.enabled}")
    private Boolean tlsEnabled;

    @PostConstruct
    private void checkForTls() {
        if (!tlsEnabled) {
            log.warn("You are using agent commands without TLS. This means all commands and their responses will be sent unencrypted in plaintext over the network. Check with the documentation on how to enable TLS.");
        }
    }

    /**
     * Keys are agent-ids and values the corresponding StreamObserver that can be used to send commands to that agent.
     */
    BiMap<String, StreamObserver<Command>> agentConnections = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * Dispatches a given command to the given agent, and adds a CommandCallBack to the CallbackManager for later handling of the response.
     *
     * @param agentId Id of the agent that should execute the command.
     * @param command The command to be executed by the agent.
     *
     * @return Returns DeferredResult that will be filled later with the agent's response.
     */
    public DeferredResult<ResponseEntity<?>> dispatchCommand(String agentId, Command command) {

        // Prepare response
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

    /**
     * Bi-directional streaming gRPC call for exchanging agent commands and responses.
     *
     * @param commandsObserver StreamObserver that the server can use to send commands to the agent that called this method.
     *
     * @return Returns StreamObserver that the agent that called this method can use to send responses to the server.
     */
    @Override
    public StreamObserver<CommandResponse> askForCommands(StreamObserver<Command> commandsObserver) {
        return new StreamObserver<CommandResponse>() {

            @Override
            public void onNext(CommandResponse commandResponse) {
                if (commandResponse.hasFirst()) {
                    // In its first message the agent will tell the server its ID
                    String agentId = commandResponse.getFirst().getAgentId();
                    log.debug("New agent '{}' connected itself to config-server.", agentId);
                    // To be able to connect incoming commands with the correct connection to the given agent,
                    // the server adds the connection to agentConnections
                    agentConnections.put(agentId, commandsObserver);
                } else {
                    // If the message is not the first one, it will contain a CommandResponse
                    String agentId = agentConnections.inverse().get(commandsObserver);
                    log.debug("Agent '{}' answered to '{}'.", agentId, commandResponse.getCommandId());
                    callbackManager.handleCommandResponse(UUID.fromString(commandResponse.getCommandId()), commandResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                String agentId = agentConnections.inverse().get(commandsObserver);
                log.error("Encountered error in askForCommands ending the stream connection with agent {}. {}", agentId, t.toString());
                // Encountering an error closes the connection, so it also needs to be removed from agentConnections.
                agentConnections.remove(agentId);
            }

            @Override
            public void onCompleted() {
                // The agent will call this when it ends its AgentCommandService.
                String agentId = agentConnections.inverse().get(commandsObserver);
                log.debug("Agent '{}' ended the stream connection.", agentId);
                // When this method is called, the connection is closed, so it also needs to be removed from agentConnections.
                agentConnections.remove(agentId);
            }
        };
    }

    /**
     * Customizes the created gRPC server to make the maxInboundMessageSize configurable.
     *
     * @return Returns customized GrpcServerConfigurer.
     */
    @Bean
    public GrpcServerConfigurer grpcServerConfigurer() {
        return serverBuilder -> {
            if (serverBuilder instanceof NettyServerBuilder) {
                int inboundMessageSizeInBytes = configuration.getAgentCommand()
                        .getMaxInboundMessageSize() * 1024 * 1024;
                ((NettyServerBuilder) serverBuilder).maxInboundMessageSize(inboundMessageSizeInBytes);
            }
        };
    }
}