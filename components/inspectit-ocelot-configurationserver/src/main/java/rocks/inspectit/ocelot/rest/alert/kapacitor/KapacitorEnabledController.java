package rocks.inspectit.ocelot.rest.alert.kapacitor;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.KapacitorState;

/**
 * Controller provided an Endpoint for checking whether the kapacitor connection is enabled.
 */
@RestController
@Slf4j
public class KapacitorEnabledController extends KapacitorBaseController {

    @Autowired
    public KapacitorEnabledController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides Information about the configured Kapacitor connection.")
    @GetMapping("/alert/kapacitor")
    public KapacitorState getState() {
        boolean kapacitorEnabled = isKapacitorEnabled();
        boolean kapacitorReachable = false;
        if (kapacitorEnabled) {
            try {
                ResponseEntity<Void> response = kapacitor().getForEntity("/kapacitor/v1/ping", Void.class);
                kapacitorReachable = response.getStatusCode().is2xxSuccessful();
            } catch (Exception exception) {
                log.debug("Assuming Kapacitor is unreachable", exception);
            }
        }
        return KapacitorState.builder()
                .enabled(kapacitorEnabled)
                .kapacitorOnline(kapacitorReachable)
                .build();
    }

}
