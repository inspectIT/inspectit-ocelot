package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import javax.annotation.PostConstruct;
import java.util.HashMap;

@Component
public class MetricWritingHealthEventListener implements HealthEventListener {

    private static final String INITIAL_METRIC_MESSAGE = "Initial health metric sent";

    @Autowired
    private SelfMonitoringService selfMonitoringService;

    @PostConstruct
    @VisibleForTesting
    void sendInitialHealthMetric() {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("message", INITIAL_METRIC_MESSAGE);
        selfMonitoringService.recordMeasurement("health", AgentHealth.OK.ordinal(), tags);
    }
    @Override
    public void onAgentHealthEvent(AgentHealthChangedEvent event) {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("message", event.getMessage());
        tags.put("source", event.getSource().getClass().getName());
        selfMonitoringService.recordMeasurement("health", event.getNewHealth().ordinal(), tags);
    }
}
