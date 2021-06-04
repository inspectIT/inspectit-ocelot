package rocks.inspectit.ocelot.core.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for fetching agent commands.
 */
@Service
@Slf4j
public class AgentCommandService extends DynamicallyActivatableService implements Runnable {

    @Autowired
    private ScheduledExecutorService executor;

    /**
     * The state of the used HTTP property source configuration.
     */
    @Autowired
    private CommandHandler commandHandler;

    /**
     * The scheduled task.
     */
    private ScheduledFuture<?> handlerFuture;

//    /**
//     * Whether the agent is in the live mode.
//     */
//    private long liveModeStart = 0L;

    public AgentCommandService() {
        super("agent-commands");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return configuration.getAgentCommands().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Starting agent command polling service.");

        AgentCommandSettings settings = configuration.getAgentCommands();
        long pollingIntervalMs = settings.getPollingInterval().toMillis();

        handlerFuture = executor.scheduleWithFixedDelay(this, pollingIntervalMs, pollingIntervalMs, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping agent command polling service.");

        if (handlerFuture != null) {
            handlerFuture.cancel(true);
        }
        return true;
    }

    @Override
    public void run() {
        log.debug("Trying to fetch new agent commands.");
        try {
            commandHandler.nextCommand();
        } catch (Exception exception) {
            log.error("Error while fetching agent command.", exception);
        }

        //        AgentCommandSettings settings = env.getCurrentConfig().getAgentCommands();
        //
        //        boolean inLiveMode = liveModeStart <= 0L;
        //        boolean keepLiveMode = commandHandler.nextCommand(inLiveMode);
        //
        //        boolean immediateExecution;
        //        if (keepLiveMode) {
        //            liveModeStart = System.currentTimeMillis();
        //            immediateExecution = true;
        //        } else {
        //            Duration liveModeDuration = settings.getLiveModeDuration();
        //            boolean liveExpired = System.currentTimeMillis() > liveModeStart + liveModeDuration.toMillis();
        //
        //            if (liveExpired) {
        //                liveModeStart = 0L;
        //                immediateExecution = false;
        //            } else {
        //                immediateExecution = true;
        //            }
        //        }
        //
        //        if (settings.isEnabled()) {
        //            if (immediateExecution) {
        //                pollerFuture = null;
        //                executor.execute(this);
        //            } else {
        //                long pollingIntervalMs = settings.getPollingInterval().toMillis();
        //                pollerFuture = executor.schedule(this, pollingIntervalMs, TimeUnit.MILLISECONDS);
        //            }
        //        }
    }
}
