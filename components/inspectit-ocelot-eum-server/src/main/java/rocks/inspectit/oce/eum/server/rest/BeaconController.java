package rocks.inspectit.oce.eum.server.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.processor.CompositeBeaconProcessor;
import rocks.inspectit.oce.eum.server.exporters.beacon.BeaconHttpExporter;
import rocks.inspectit.oce.eum.server.metrics.BeaconMetricManager;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

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

    @Autowired(required = false)
    private BeaconHttpExporter beaconHttpExporter;

    @ExceptionHandler({Exception.class})
    public void handleException(Exception exception) {
        selfMonitoringService.record("beacons_received", 1, Collections.singletonMap("is_error", "true"));
        log.error("Error while receiving beacon", exception);
    }

    @CrossOrigin
    @PostMapping("beacon")
    public ResponseEntity beaconPost(@RequestBody MultiValueMap<String, String> formData) {
        return processBeacon(formData);
    }

    @CrossOrigin
    @GetMapping("beacon")
    public ResponseEntity beaconGet(@RequestParam MultiValueMap<String, String> requestParams) {
        return processBeacon(requestParams);
    }

    /**
     * Processes the incoming beacon data.
     *
     * @param beaconData the received EUM data
     *
     * @return the response used as result for the request
     */
    private ResponseEntity<Object> processBeacon(MultiValueMap<String, String> beaconData) {
        Beacon beacon = beaconProcessor.process(Beacon.of(beaconData.toSingleValueMap()));

        // export beacon
        if (beaconHttpExporter != null) {
            beaconHttpExporter.export(beacon);
        }

        // record metrics based on beacon data
        boolean successful = beaconMetricManager.processBeacon(beacon);

        selfMonitoringService.record("beacons_received", 1, Collections.singletonMap("is_error", String.valueOf(!successful)));

        return ResponseEntity.ok().build();
    }
}