package rocks.inspectit.ocelot.config.model.privacy.obfuscation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * This pattern defines a regular expression that should be used to check if data is considered as private and thus
 * obfuscation is needed.
 * <p>
 * The boolean values {@link #checkKey} and {@link #checkData} defines what values should the pattern be tested against:
 * - checkKey - checks only the value of the key (in key-value stores)
 * - checkData - checks the value of the value (in key-value stores) or general data pushed (no key-value stores)
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ObfuscationPattern {

    /**
     * Regular expression to apply.
     */
    @NotNull
    @NotBlank
    private String pattern;

    /**
     * If key of the data should be checked for obfuscation by this pattern.
     */
    private boolean checkKey = true;

    /**
     * If data itself should be checked for obfuscation by this pattern.
     */
    private boolean checkData = false;

}
