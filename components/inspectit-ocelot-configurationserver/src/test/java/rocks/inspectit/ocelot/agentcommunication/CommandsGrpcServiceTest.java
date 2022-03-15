package rocks.inspectit.ocelot.agentcommunication;

import com.google.common.collect.BiMap;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.grpc.AgentCommandsGrpc;
import rocks.inspectit.ocelot.grpc.Command;
import rocks.inspectit.ocelot.grpc.CommandResponse;
import rocks.inspectit.ocelot.grpc.FirstResponse;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandsGrpcServiceTest {

    private static final String AGENT_ID = "agent-id";

    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private BiMap<String, StreamObserver<Command>> agentConnections;

    @Mock
    private AgentCallbackManager callbackManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitServerSettings configuration;

    @InjectMocks
    private CommandsGrpcService service;

    private ManagedChannel inProcessChannel;

    @BeforeEach
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        Server server = InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build();
        server.start();

        // Create a client channel and register for automatic graceful shutdown.
        inProcessChannel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    }

    @Nested
    public class AskForCommandsTest {

        @Test
        void FirstResponse() {

            StreamObserver<Command> commandObserver = mock(StreamObserver.class);
            AgentCommandsGrpc.AgentCommandsStub stub = AgentCommandsGrpc.newStub(inProcessChannel);

            StreamObserver<CommandResponse> responseObserver = stub.askForCommands(commandObserver);
            verify(commandObserver, never()).onNext(any());

            responseObserver.onNext(CommandResponse.newBuilder()
                    .setFirst(FirstResponse.newBuilder().setAgentId(AGENT_ID))
                    .build());

            verify(agentConnections, times(1)).put(eq(AGENT_ID), any());
        }

        @Test
        public void DispatchCommand() {

            StreamObserver<Command> commandObserver = mock(StreamObserver.class);
            AgentCommandsGrpc.AgentCommandsStub stub = AgentCommandsGrpc.newStub(inProcessChannel);

            StreamObserver<CommandResponse> responseObserver = stub.askForCommands(commandObserver);
            verify(commandObserver, never()).onNext(any());

            when(agentConnections.get(AGENT_ID)).thenReturn(commandObserver);
            when(configuration.getAgentCommand().getResponseTimeout()).thenReturn(Duration.ofMillis(1000));

            Command command = Command.newBuilder().setCommandId(UUID.randomUUID().toString()).build();
            service.dispatchCommand(AGENT_ID, command);

            verify(agentConnections).get(AGENT_ID);

            ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(commandObserver).onNext(commandCaptor.capture());

            assertEquals(command, commandCaptor.getValue());
        }
    }
}