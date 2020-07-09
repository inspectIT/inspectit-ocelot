package rocks.inspectit.ocelot.rest.alert.kapacitor;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.util.Collections;
import java.util.Map;

/**
 * Controller provided an Endpoint for checking whether the kapacitor connection is enabled.
 */
@RestController
public class KapacitorEnabledController extends KapacitorBaseController {

    @Autowired
    public KapacitorEnabledController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides Information about the configured Kapacitor connection.")
    @GetMapping("/alert/kapacitor")
    public Map<String, Object> getState() {
        return Collections.singletonMap("enabled", isKapacitorEnabled());
    }

}
