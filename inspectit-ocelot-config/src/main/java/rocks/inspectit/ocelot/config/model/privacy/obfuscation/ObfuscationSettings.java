package rocks.inspectit.ocelot.config.model.privacy.obfuscation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class ObfuscationSettings {

    /**
     * If obfuscation is enabled.
     */
    private boolean enabled = true;

    /**
     * If all patterns should be considered as case insensitive.
     */
    private boolean caseInsensitive = true;

    /**
     * Patterns that should be used for checking if obfuscation is needed. Ignored if {@link #enabled} is <code>false</code>.
     */
    @Valid
    @NotNull
    private List<ObfuscationPattern> patterns = Collections.emptyList();

}
