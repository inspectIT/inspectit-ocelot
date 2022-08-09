package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import java.util.HashMap;

public class MetricWritingHealthEventListener implements HealthEventListener {

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    @Override
    public void onAgentHealthEvent(AgentHealthChangedEvent event) {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("message", event.getMessage());
        selfMonitoringService.recordMeasurement("health", event.getNewHealth().ordinal(), tags);
    }
}
