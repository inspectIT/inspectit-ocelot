package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

/**
 * Configuration object for all settings regarding the instrumentation.
 */
@Data
@NoArgsConstructor
public class InstrumentationSettings {

    /**
     * The configuration of internal parameters regarding the instrumentation process.
     */
    @Valid
    private InternalSettings internal;

    /**
     * The configuration for all special sensors.
     */
    @Valid
    private SpecialSettings special;

}
