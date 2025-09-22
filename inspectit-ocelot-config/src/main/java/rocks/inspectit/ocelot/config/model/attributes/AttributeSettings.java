package rocks.inspectit.ocelot.config.model.attributes;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.attributes.providers.AttributesProvidersSettings;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class AttributeSettings {

    /**
     * Settings for available attribute providers.
     */
    @Valid
    private AttributesProvidersSettings providers;

    /**
     * Map of arbitrary user defined attributes.
     */
    private Map<String, String> extra = new HashMap<>();
}
