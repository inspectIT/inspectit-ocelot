package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.List;

/**
 * Beacon Processor acting as composite component to invoke all available {@link BeaconProcessor}s.
 */
@Component
@Slf4j
public class CompositeBeaconProcessor implements BeaconProcessor {

    @Autowired
    private List<BeaconProcessor> processorList;

    @Override
    public Beacon process(Beacon beacon) {
        for (BeaconProcessor beaconProcessor : processorList) {
            try {
                beacon = beacon.merge(beaconProcessor.process(beacon));
            } catch (Exception e) {
                log.error("BeaconProcessor <{}> encountered an Exception! Ignoring the processor!", beaconProcessor.getClass().getName(), e);
            }
        }
        return beacon;
    }
}
