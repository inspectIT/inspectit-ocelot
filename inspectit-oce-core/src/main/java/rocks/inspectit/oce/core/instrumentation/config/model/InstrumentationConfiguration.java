package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;

import java.util.Map;
import java.util.Set;

/**
 * Contains the resolved instrumentation settings.
 * The difference between resolved and unresolved configurations (= {@link rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings}
 * is that preprocessing by the {@link InstrumentationConfigurationResolver} has occurred.
 * For example, in the resolved InstrumentationConfiguration profiles have been unrolled, so that a complete set of rules is available.
 */
@Value
@Builder
public class InstrumentationConfiguration {

    /**
     * The instrumentation settings which have been used to derive this configuration.
     */
    private InstrumentationSettings source;

    /**
     * The currently active instrumentation rules.
     */
    @Singular
    private Set<InstrumentationRule> rules;

    /**
     * A map mapping the name of each data provider to its resolved configuration.
     * This map does not include special builtin providers!
     * <p>
     * The key is stored in lower-case to allow an case insensitive matching.
     */
    @Singular
    private Map<String, ResolvedGenericDataProviderConfig> dataProviders;
}
