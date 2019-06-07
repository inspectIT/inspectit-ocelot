package rocks.inspectit.oce.eum.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.oce.eum.server.metrics.MeasuresAndViewsManager;

@RestController()
@RequestMapping("/")
public class BeaconController {

    @Autowired
    MeasuresAndViewsManager measuresAndViewsManager;


    @RequestMapping(method = RequestMethod.POST, value = "beacon")
    public ResponseEntity postBeacon(@RequestBody MultiValueMap<String, String> formData) {
        measuresAndViewsManager.processBeacon(formData.toSingleValueMap());
        return ResponseEntity.accepted().build();
    }

}