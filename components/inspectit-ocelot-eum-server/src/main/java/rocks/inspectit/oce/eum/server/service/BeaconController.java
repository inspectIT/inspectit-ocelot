package rocks.inspectit.oce.eum.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.metrics.BeaconMetricManager;
import rocks.inspectit.oce.eum.server.metrics.BeaconPreProcessor;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

@RestController()
@RequestMapping("/")
public class BeaconController {

    @Autowired
    private BeaconPreProcessor beaconPreProcessor;

    @Autowired
    private BeaconMetricManager beaconMetricManager;

    @Autowired
    SelfMonitoringMetricManager selfMonitoringService;

    @CrossOrigin
    @PostMapping("beacon")
    public ResponseEntity postBeacon(@RequestBody MultiValueMap<String, String> formData) {
        Beacon beacon = beaconPreProcessor.preProcessBeacon(formData.toSingleValueMap());
        beaconMetricManager.processBeacon(beacon);
        selfMonitoringService.record("beacons_received", 1);
        return ResponseEntity.ok().build();
    }
}