package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Value;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;

/**
 * Contains the resolved instrumentation settings.
 * The difference between resolved and unresolved configurations (= {@link rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings}
 * is that preprocessing by the {@link InstrumentationConfigurationResolver} has occurred.
 * For example, in the resolved InstrumentationConfiguration profiles have been unrolled, so that a complete set of rules is available.
 */
@Value
public class InstrumentationConfiguration {

    /**
     * The instrumentation settings which have been used to derive this configuration.
     */
    private InstrumentationSettings source;
}
