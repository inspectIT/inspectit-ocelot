package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Beacon Processor acting as composite component to invoke all available {@link BeaconProcessor}s.
 */
@Component
@Slf4j
public class CompositeBeaconProcessor implements BeaconProcessor {

    @Autowired
    private List<BeaconProcessor> processorList;

    @Autowired
    private SelfMonitoringMetricManager selfMonitoring;

    @Override
    public Beacon process(Beacon beacon) {
        for (BeaconProcessor beaconProcessor : processorList) {
            boolean is_error = false;
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                beacon = beacon.merge(beaconProcessor.process(beacon));
            } catch (Exception e) {
                log.error("BeaconProcessor <{}> encountered an Exception! Ignoring the processor!", beaconProcessor.getClass()
                        .getName(), e);
                is_error = true;
            }
            stopwatch.stop();
            ImmutableMap<String, String> tagMap = ImmutableMap.of("beacon_processor", beaconProcessor.getClass()
                    .getSimpleName(), "is_error", String.valueOf(is_error));
            selfMonitoring.record("beacons_processor", stopwatch.elapsed(TimeUnit.MILLISECONDS), tagMap);
        }
        return beacon;
    }
}
