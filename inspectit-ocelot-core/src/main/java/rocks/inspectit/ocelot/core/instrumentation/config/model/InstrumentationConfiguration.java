package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import rocks.inspectit.ocelot.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;

import java.util.Set;

/**
 * Contains the resolved instrumentation settings.
 * The difference between resolved and unresolved configurations (= {@link InstrumentationSettings}
 * is that preprocessing by the {@link InstrumentationConfigurationResolver} has occurred.
 * For example, in the resolved InstrumentationConfiguration profiles have been unrolled, so that a complete set of rules is available.
 */
@Value
@Builder
@NonFinal //for testing
public class InstrumentationConfiguration {

    /**
     * The instrumentation settings which have been used to derive this configuration.
     */
    private InstrumentationSettings source;

    private DataProperties dataProperties;

    /**
     * The currently active instrumentation rules.
     */
    @Singular
    private Set<InstrumentationRule> rules;
}
