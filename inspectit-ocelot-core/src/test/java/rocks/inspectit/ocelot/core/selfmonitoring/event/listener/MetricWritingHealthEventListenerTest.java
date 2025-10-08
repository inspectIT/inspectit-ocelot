package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.selfmonitoring.event.models.AgentHealthChangedEvent;

import java.util.HashMap;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetricWritingHealthEventListenerTest {

    static final String INITIAL_METRIC_MESSAGE = "Initial health metric sent";

    @InjectMocks
    MetricWritingHealthEventListener metricWritingHealthEventListener;

    @Mock
    SelfMonitoringService selfMonitoringService;

    @Test
    void sendInitialHealthMetric() {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("message", INITIAL_METRIC_MESSAGE);

        metricWritingHealthEventListener.sendInitialHealthMetric();

        verify(selfMonitoringService).recordMetric("health", AgentHealth.OK.ordinal(), tags);
    }

    @Test
    void recordNewHealthMeasurement() {
        AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, AgentHealth.OK, AgentHealth.WARNING, "Mock Message");
        HashMap<String, String> tags = new HashMap<>();
        tags.put("message", event.getMessage());
        tags.put("source", event.getSource().getClass().getName());

        metricWritingHealthEventListener.onAgentHealthEvent(event);

        verify(selfMonitoringService).recordMetric("health", event.getNewHealth().ordinal(), tags);
    }
}
