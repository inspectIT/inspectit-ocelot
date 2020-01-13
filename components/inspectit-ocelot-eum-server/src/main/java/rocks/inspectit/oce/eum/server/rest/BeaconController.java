package rocks.inspectit.oce.eum.server.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.metrics.BeaconMetricManager;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;
import rocks.inspectit.oce.eum.server.beacon.processor.CompositeBeaconProcessor;

import java.util.Collections;

@RestController()
@RequestMapping("/")
@Slf4j
public class BeaconController {

    @Autowired
    private BeaconMetricManager beaconMetricManager;

    @Autowired
    private CompositeBeaconProcessor beaconProcessor;

    @Autowired
    private SelfMonitoringMetricManager selfMonitoringService;

    @CrossOrigin
    @PostMapping("beacon")
    public ResponseEntity postBeacon(@RequestBody MultiValueMap<String, String> formData) {
        Beacon beacon = beaconProcessor.process(Beacon.of(formData.toSingleValueMap()));
        boolean successful = beaconMetricManager.processBeacon(beacon);

        selfMonitoringService.record("beacons_received", 1, Collections.singletonMap("is_error", String.valueOf(!successful)));

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({Exception.class})
    public void handleException(Exception exception) {
        selfMonitoringService.record("beacons_received", 1, Collections.singletonMap("is_error", "true"));
        log.error("Error while receiving beacon", exception);
    }
}