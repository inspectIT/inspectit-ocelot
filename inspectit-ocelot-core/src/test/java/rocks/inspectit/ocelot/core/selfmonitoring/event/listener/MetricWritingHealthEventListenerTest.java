package rocks.inspectit.ocelot.core.selfmonitoring.event.listener;

import org.junit.jupiter.api.Nested;
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
public class MetricWritingHealthEventListenerTest {

    private static final String INITIAL_METRIC_MESSAGE = "Initial health metric sent";

    @InjectMocks
    private MetricWritingHealthEventListener metricWritingHealthEventListener;

    @Mock
    private SelfMonitoringService selfMonitoringService;

    @Nested
    class SendInitialHealthMetric {

        @Test
        void successfulDelegationVerification() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("message", INITIAL_METRIC_MESSAGE);

            metricWritingHealthEventListener.sendInitialHealthMetric();

            verify(selfMonitoringService).recordMeasurement("health", AgentHealth.OK.ordinal(), tags);
        }
    }

    @Nested
    class OnAgentHealthEvent {

        @Test
        void recordNewHealthMeasurement() {
            AgentHealthChangedEvent event = new AgentHealthChangedEvent(this, AgentHealth.OK, AgentHealth.WARNING, "Mock Message");
            HashMap<String, String> tags = new HashMap<>();
            tags.put("message", event.getMessage());
            tags.put("source", event.getSource().getClass().getName());

            metricWritingHealthEventListener.onAgentHealthEvent(event);

            verify(selfMonitoringService).recordMeasurement("health", event.getNewHealth().ordinal(), tags);
        }
    }

}
