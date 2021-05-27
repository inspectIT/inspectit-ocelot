package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CompositeBeaconProcessorTest {

    @InjectMocks
    private CompositeBeaconProcessor processor;

    @Mock
    private SelfMonitoringMetricManager selfMonitoring;

    @Spy
    private List<BeaconProcessor> processorList = ImmutableList.of(beacon -> beacon.merge(ImmutableMap.of("key2", "value2")), beacon -> beacon
            .merge(ImmutableMap.of("key1", "value2")));

    @Test
    public void test() {
        Beacon processedBeacon = processor.process(Beacon.of(ImmutableMap.of("key1", "value1")));

        // Ensure value got properly overwritten
        Assertions.assertThat(processedBeacon.get("key1")).isEqualTo("value2");
        // Ensure new key was properly added
        Assertions.assertThat(processedBeacon.get("key2")).isEqualTo("value2");
    }

}