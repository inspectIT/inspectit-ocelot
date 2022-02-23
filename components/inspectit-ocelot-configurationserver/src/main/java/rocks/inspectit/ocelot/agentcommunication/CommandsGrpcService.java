package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.config.model.AgentCommandSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.CommandsGrpc;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@GrpcService
@Slf4j
public class CommandsGrpcService extends CommandsGrpc.CommandsImplBase {

    @Autowired
    private InspectitServerSettings configuration;

    /**
     * Keys are agent-ids and values the corresponding StreamObserver that can be used to send commands.
     */
    BiMap<String, StreamObserver<Commands.Command>> agentConnections = Maps.synchronizedBiMap(HashBiMap.create());

    LoadingCache<String, BlockingQueue<Commands.Command>> agentCommandCache;

    @PostConstruct
    public void postConstruct() {
        AgentCommandSettings commandSettings = configuration.getAgentCommand();
        long commandTimeout = commandSettings.getCommandTimeout().toMillis();
        int commandQueueSize = commandSettings.getCommandQueueSize();

        agentCommandCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(commandTimeout, TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, BlockingQueue<Commands.Command>>() {
                    @Override
                    public BlockingQueue<Commands.Command> load(String key) {
                        return new LinkedBlockingQueue<>(commandQueueSize);
                    }
                });
    }

    public void dispatchCommand(Commands.Command command, String agentId) throws ExecutionException {
        BlockingQueue<Commands.Command> commandQueue = agentCommandCache.get(agentId);
        synchronized (commandQueue) {
            boolean success = commandQueue.offer(command);
            if (success && commandQueue.size() == 1) {
                StreamObserver<Commands.Command> commandsObserver = agentConnections.get(agentId);
                commandsObserver.onNext(command);
            }
        }
    }

    private void sendNextCommandToAgent(String agentId) throws ExecutionException {
        StreamObserver<Commands.Command> commandsObserver = agentConnections.get(agentId);
        BlockingQueue<Commands.Command> commandQueue = agentCommandCache.get(agentId);
        Commands.Command newCommand = commandQueue.peek();
    }

    @Override
    public StreamObserver<Commands.CommandResponse> askForCommands(StreamObserver<Commands.Command> responseObserver) {
        return new StreamObserver<Commands.CommandResponse>() {
            @Override
            public void onNext(Commands.CommandResponse value) {
                if (value.hasFirst()) {
                    agentConnections.put(value.getFirst().getAgentId(), responseObserver);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.info("Encountered error in exchangeInformation: {}", t.toString());
            }

            @Override
            public void onCompleted() {
                String agentId = agentConnections.inverse().get(responseObserver);
                log.info("Agent {} ended Commands Stream Connection.", agentId);
                agentConnections.remove(agentId);
                responseObserver.onCompleted();
            }
        };
    }
}