package rocks.inspectit.oce.eum.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.oce.eum.server.metrics.BeaconPreProcessor;
import rocks.inspectit.oce.eum.server.metrics.MeasuresAndViewsManager;

import java.util.Map;

@RestController()
@RequestMapping("/")
public class BeaconController {

    @Autowired
    BeaconPreProcessor beaconPreProcessor;

    @Autowired
    MeasuresAndViewsManager measuresAndViewsManager;

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, value = "beacon")
    public ResponseEntity postBeacon(@RequestBody MultiValueMap<String, String> formData) {
        Map<String, String> beacon = beaconPreProcessor.preProcessBeacon(formData.toSingleValueMap());
        measuresAndViewsManager.processBeacon(beacon);
        return ResponseEntity.accepted().build();
    }
}