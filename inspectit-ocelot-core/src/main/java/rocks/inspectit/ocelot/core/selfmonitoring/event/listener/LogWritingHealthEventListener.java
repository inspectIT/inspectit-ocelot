package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

@Slf4j
public class LogWritingHealthEventListener implements HealthEventListener {

    private static final String LOG_CHANGE_STATUS = "The agent status changed from {} to {}. Reason: {}";

    @Override
    public void onAgentHealthEvent(AgentHealthChangedEvent event) {

        if (event.getNewHealth().isMoreSevereOrEqualTo(event.getOldHealth())) {
            log.warn(LOG_CHANGE_STATUS, event.getOldHealth(), event.getOldHealth(), event.getMessage());
        } else {
            log.info(LOG_CHANGE_STATUS, event.getOldHealth(), event.getOldHealth(), event.getMessage());
        }

    }
}
