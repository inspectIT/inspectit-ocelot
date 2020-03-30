package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * Resource timing settings.
 */
@Data
@Validated
public class ResourceTimingSettings {

    /**
     * If resource timing is enabled or not.
     */
    private boolean enabled = true;

}
