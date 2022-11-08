package rocks.inspectit.ocelot.core.command;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.grpc.*;

import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class AgentCommandClient {

    /**
     * Timestamp of when the client last tried to re-establish the connection to the server.
     */
    private long lastRetryAttempt;

    /**
     * ScheduledThreadPoolExecutor for connection re-establishing threads.
     */
    ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);

    /**
     * Number of retries attempted. Is reset if no retries happened for the time set in {@link AgentCommandSettings#getBackoffResetTime()}.
     */
    private int retriesAttempted = 0;

    /**
     * Asynchronous stub used to perform the gRPC call to the config-server.
     */
    AgentCommandsGrpc.AgentCommandsStub asyncStub;

    /**
     * The agent's ID based on RuntimeMXBean name.
     */
    String agentId;

    /**
     * StreamObserver to send messages, i.e. first message and command responses, to the config-server.
     * Volatile because of the possibility of retrying connecting and disabling at the same time.
     */
    volatile private StreamObserver<CommandResponse> commandResponseObserver = null;

    AgentCommandClient(Channel channel) {
        asyncStub = AgentCommandsGrpc.newStub(channel);
        agentId = ManagementFactory.getRuntimeMXBean().getName();
    }

    /**
     * Establishes the bi-directional streaming connection to the config-server by calling the gRPC call askForCommands.
     *
     * @param settings         AgentCommandSettings.
     * @param commandDelegator CommandDelegator to delegate the received commands with.
     *
     * @return Boolean whether the connection was established successfully.
     */
    boolean startAskForCommandsConnection(AgentCommandSettings settings, CommandDelegator commandDelegator) {
        try {
            StreamObserver<Command> commandObserver = new StreamObserver<Command>() {
                @Override
                public void onNext(Command command) {
                    try {
                        log.debug("Received command '{}' from config-server.", command.getCommandId());
                        commandResponseObserver.onNext(commandDelegator.delegate(command));
                        log.debug("Answered to command '{}'.", command.getCommandId());
                    } catch (Exception exception) {
                        // Send response with Error message.
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
                    if (commandResponseObserver == null) {
                        log.error("Error while trying to send onCompleted() to server. Will end connection without retrying.", t);
                    } else {
                        log.error("Encountered error in askForCommands ending the stream connection with config-Server.", t);
                        reestablishConnection(settings, commandDelegator);
                    }
                }

                // The server will never call this, so it is left empty.
                @Override
                public void onCompleted() {
                }

            };

            commandResponseObserver = asyncStub.askForCommands(commandObserver);

            // Send the first message with the agent's ID.
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

    /**
     * Re-establishes the connection to the config-server after a backoff based on how often it has retried already.
     *
     * @param settings         AgentCommandSettings needed to call {@link this#startAskForCommandsConnection(AgentCommandSettings, CommandDelegator)} again.
     * @param commandDelegator CommandDelegator needed to call {@link this#startAskForCommandsConnection(AgentCommandSettings, CommandDelegator)} again.
     */
    private void reestablishConnection(AgentCommandSettings settings, CommandDelegator commandDelegator) {
        log.info("Re-establishing gRPC connection.");

        long currentTime = System.nanoTime();

        // Check whether number of retries should be reset.
        if (retriesAttempted > 0) {
            if ((currentTime - lastRetryAttempt) > settings.getBackoffResetTime().toNanos()) {
                retriesAttempted = 0;
            }
        }

        // Re-try establishing the connection after backoff.
        ScheduledFuture<Boolean> restartFuture = reconnectExecutor.schedule(() -> startAskForCommandsConnection(settings, commandDelegator), (long) Math.pow(2, retriesAttempted + 1), TimeUnit.SECONDS);

        // Raise retries attempted if set maximum has not been reached yet.
        if (retriesAttempted < settings.getMaxBackoffIncreases()) {
            retriesAttempted++;
        }

        lastRetryAttempt = currentTime;

        boolean successful;
        try {
            successful = restartFuture.get();
        } catch (Exception e) {
            successful = false;
        }

        // If it was not successful try again.
        if (!successful) {
            reestablishConnection(settings, commandDelegator);
        }
    }
    
    /**
     * Shut the connection to the config-server down.
     */
    @PreDestroy
    void shutdown() {
        // Stop any existing tries to re-establish the connection.
        reconnectExecutor.shutdownNow();

        if (commandResponseObserver != null) {
            try {
                commandResponseObserver.onCompleted();
            } catch (NullPointerException e) {
                // It could still happen that commandResponseObserver is set to null by another thread that could not be
                // stopped by reconnectExecutor.shutdownNow() yet right after checking it in the if statement.
                // This is only dealt with using a try-catch-block instead of locking because that happening does not
                // lead to any further problems.
                log.debug("Encountered null exception while disabling agent command service, probably because a thread trying to restart the connection was still running and modifying commandResponseObserver at the same time.", e);
            }
            commandResponseObserver = null;
        }
    }
}
