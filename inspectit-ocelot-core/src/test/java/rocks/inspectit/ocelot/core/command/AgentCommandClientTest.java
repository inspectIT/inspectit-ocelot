package rocks.inspectit.ocelot.core.command;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.grpc.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentCommandClientTest {

    @Nested
    public class AskForCommandsTest {

        /**
         * This rule manages automatic graceful shutdown for the registered servers and channels at the
         * end of test.
         */
        @Rule
        public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

        private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

        AgentCommandClient client;

        List<CommandResponse> responseList = new ArrayList<>();

        @BeforeEach
        public void setUp() throws Exception {
            // Generate a unique in-process server name.
            String serverName = InProcessServerBuilder.generateName();

            // Create a server, add service, start, and register for automatic graceful shutdown.
            grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                    .fallbackHandlerRegistry(serviceRegistry)
                    .directExecutor()
                    .build()
                    .start());

            // Create a client channel and register for automatic graceful shutdown.
            ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                    .directExecutor()
                    .build());

            // Create a AgentCommandClient using the in-process channel;
            client = new AgentCommandClient(channel);
        }

        @Test
        void CommandExchangeTest() {

            AgentCommandSettings settings = mock(AgentCommandSettings.class);
            AgentCommandService commandService = mock(AgentCommandService.class);
            CommandDelegator delegator = mock(CommandDelegator.class);

            AgentCommandsGrpc.AgentCommandsImplBase serviceImpl = new AgentCommandsGrpc.AgentCommandsImplBase() {
                @Override
                public StreamObserver<CommandResponse> askForCommands(StreamObserver<Command> responseObserver) {
                    return new StreamObserver<CommandResponse>() {
                        @Override
                        public void onNext(CommandResponse value) {
                            responseList.add(value);
                            if (value.hasFirst()) {
                                responseObserver.onNext(Command.newBuilder()
                                        .setCommandId("id")
                                        .setPing(PingCommand.newBuilder())
                                        .build());
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onCompleted() {
                        }
                    };
                }
            };

            serviceRegistry.addService(serviceImpl);

            CommandResponse response = CommandResponse.newBuilder()
                    .setCommandId("id")
                    .setPing(PingCommandResponse.newBuilder())
                    .build();
            when(commandService.getCommandDelegator()).thenReturn(delegator);
            when(delegator.delegate(any())).thenReturn(response);

            client.startAskForCommandsConnection(settings, commandService.getCommandDelegator());

            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            String agentId = runtime.getName();

            assertEquals(Arrays.asList(CommandResponse.newBuilder()
                    .setFirst(FirstResponse.newBuilder().setAgentId(agentId))
                    .build(), response), responseList);
        }
    }
}