package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.Singular;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Resource timing settings.
 */
@Data
@Validated
public class ResourceTimingSettings {

    /**
     * If resource timing is enabled or not.
     */
    private boolean enabled;

    @Singular
    private Map<@NotBlank String, @NotNull Boolean> tags;

}
