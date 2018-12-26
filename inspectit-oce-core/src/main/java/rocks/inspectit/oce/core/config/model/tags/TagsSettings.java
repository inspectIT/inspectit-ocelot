package rocks.inspectit.oce.core.config.model.tags;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.model.tags.providers.TagsProvidersSettings;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class TagsSettings {

    /**
     * Settings for available tags providers.
     */
    @Valid
    private TagsProvidersSettings providers;

    /**
     * Map of arbitrary user defined tags.
     */
    private final Map<String, String> extra = new HashMap<>();

}
