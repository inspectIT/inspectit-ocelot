package rocks.inspectit.ocelot.config.model.privacy;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class PrivacySettings {

    /**
     * Obfuscation settings.
     */
    @Valid
    @NotNull
    private ObfuscationSettings obfuscation = new ObfuscationSettings();

}
