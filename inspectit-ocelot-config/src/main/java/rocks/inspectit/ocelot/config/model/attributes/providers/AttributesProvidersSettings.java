package rocks.inspectit.ocelot.config.model.attributes.providers;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class AttributesProvidersSettings {

    /**
     * The environment attributes providers.
     */
    @Valid
    private EnvironmentAttributesProviderSettings environment;
}
