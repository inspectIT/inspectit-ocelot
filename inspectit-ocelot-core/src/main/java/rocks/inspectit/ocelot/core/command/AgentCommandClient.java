package rocks.inspectit.ocelot.core.command;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.grpc.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class AgentCommandClient {

    private long lastRetryAttempt;

    ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);

    private int retriesAttempted = 0;

    AgentCommandsGrpc.AgentCommandsStub asyncStub;

    String agentId;

    // I think volatile is needed because of the possibility of retrying connecting and disabling at the same time
    volatile private StreamObserver<CommandResponse> commandResponseObserver = null;

    AgentCommandClient(Channel channel) {
        asyncStub = AgentCommandsGrpc.newStub(channel);

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        agentId = runtime.getName();
    }

    boolean startAskForCommandsConnection(AgentCommandSettings settings, AgentCommandService service) {
        try {

            StreamObserver<Command> commandObserver = new StreamObserver<Command>() {
                @Override
                public void onNext(Command command) {
                    try {
                        log.info("Received command '{}' from config-server.", command.getCommandId());
                        commandResponseObserver.onNext(service.getCommandDelegator().delegate(command));
                        log.info("Answered to command '{}'.", command.getCommandId());
                    } catch (Exception exception) {
                        commandResponseObserver.onNext(CommandResponse.newBuilder()
                                .setCommandId(command.getCommandId())
                                .setError(ErrorResponse.newBuilder().setMessage(exception.getMessage()))
                                .build());
                        log.error("Exception during agent command execution.", exception);
                    }
                }

                @Override
                public void onError(Throwable t) {

                    // It can happen that onError() is called after the service was disabled already, since the client in shutdown()
                    // tries to send an onCompleted() to the server, which will lead to an error if the connection is unavailable.
                    // So, if the commandResponseObserver was already set to null by shutdown(), do not retry to establish the connection
                    if (commandResponseObserver != null) {
                        log.error("Encountered error in exchangeInformation ending the stream connection with config-Server.", t);

                        long currentTime = System.nanoTime();

                        if (retriesAttempted > 0) {
                            if ((currentTime - lastRetryAttempt) > (Math.pow(10, 9) * settings.getBackoffResetTime())) {
                                retriesAttempted = 0;
                            }
                        }

                        ScheduledFuture<?> restartFuture = reconnectExecutor.schedule(() -> {
                            boolean success = startAskForCommandsConnection(settings, service);
                            if (success) {
                                log.info("Successfully restarted connection after error.");
                            } else {
                                service.disable();
                                log.info("Could not restart connection after error.");
                            }
                        }, (long) Math.pow(2, retriesAttempted + 1), TimeUnit.SECONDS);

                        if (retriesAttempted < settings.getMaxBackoffIncreases()) {
                            retriesAttempted++;
                        }

                        lastRetryAttempt = currentTime;
                    } else {
                        log.info("Error while trying to send onCompleted() to server. Will end connection without retrying.", t);
                    }
                }

                @Override
                public void onCompleted() {
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

    void shutdown() {
        reconnectExecutor.shutdownNow();

        if (commandResponseObserver != null) {
            try {
                commandResponseObserver.onCompleted();
            } catch (NullPointerException e) {
                log.debug("Encountered null exception while disabling agent command service, probably because a thread trying to restart the connection was running and modifying commandResponseObserver at the same time.", e);
            }
            commandResponseObserver = null;
        }
    }
}
