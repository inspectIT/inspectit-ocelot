package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.events.EventSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.sdk.events.Event;
import rocks.inspectit.ocelot.sdk.events.EventRegistryService;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EventExporterService  extends DynamicallyActivatableService {

    /** The queue which stores the event objects to be send. */
    private static ArrayList<Event> eventQueue = new ArrayList<Event>();

    /** The scheduler for periodically send the stored events within eventQueue towards registered exporters. */
    @Autowired
    private ScheduledExecutorService executor;

    /** The future for the scheduled task. Will be closed during doDisable instead of calling executor.shudown(). */
    private ScheduledFuture<?> schedule;

    private EventRegistryService registryService = new EventRegistryService();

    public EventExporterService() {
        super("events");
    }

    /**
     * Function called by {EventRecorder} in order to send Events.
     * @param eventObj - An Event which should be send towards exporters.
     */
    public void export(Event eventObj) {
        if(eventObj != null && !schedule.isCancelled()) {
            eventQueue.add(eventObj);
        }
    }

    private void sendToHandlers() {
        if(eventQueue.isEmpty()) {
            return;
        }
        registryService.sendEventsToExporters(eventQueue);
        eventQueue.clear();
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid EventSettings settings = configuration.getEvents();
        return settings.isEnabled()
                && settings.getFrequency() != null;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Starting EventExporter Service");
        try {
            EventSettings settings = configuration.getEvents();
            /**
             * Alternative:
             * if(executor not undefined){
             *     executor = new ScheduledExecutorService();
             *     executor.scheduleAtFixesRate(....);
             * }
             */
            schedule = executor.scheduleAtFixedRate(
                    () -> sendToHandlers(),
                    settings.getFrequency().getNano(),
                    settings.getFrequency().getNano(),
                    TimeUnit.NANOSECONDS
            );
        } catch (Throwable t) {
            log.error("EventExporter Service could not be started.", t);
            return false;
        }
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Shutting down EventExporter Service.");

        if(schedule.cancel(false)) {
            return true;
        }

        /**
         * Alternative:
         * try{
         *     executor.shutdown()
         * }
         */

        log.error("Shutting down EventExporter Service failed.");
        return false;
    }
}
