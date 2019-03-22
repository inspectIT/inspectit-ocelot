package rocks.inspectit.ocelot.core.config.model.tags.providers;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class TagsProvidersSettings {

    /**
     * The environment tags providers.
     */
    @Valid
    private EnvironmentTagsProviderSettings environment;

}
