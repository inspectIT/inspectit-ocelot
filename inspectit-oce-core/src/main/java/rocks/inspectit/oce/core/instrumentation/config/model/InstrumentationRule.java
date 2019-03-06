package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.*;
import rocks.inspectit.oce.core.config.model.instrumentation.rules.InstrumentationRuleSettings;

import java.util.Map;
import java.util.Set;

/**
 * This class represents an instrumentation rule defining a bunch of classes to instrument and to inject the dispatcher
 * hook which is used for generic data collection.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
public class InstrumentationRule {

    /**
     * The rule's name.
     */
    private String name;

    /**
     * The scope of this rule. This represents a matcher of types and methods that should be instrumented.
     */
    @Singular
    private Set<InstrumentationScope> scopes;

    /**
     * Maps data keys to the data provider call defining the value.
     * These assignments are performed in the method-entry part.
     */
    @Singular
    private Map<String, DataProviderCallConfig> entryProviders;

    /**
     * Maps data keys to the data provider call defining the value.
     * These assignments are performed in the method-exit part.
     */
    @Singular
    private Map<String, DataProviderCallConfig> exitProviders;

    /**
     * Maps metrics to the data keys or constants used as sources, see {@link InstrumentationRuleSettings#getMetrics()}.
     * However, this map is guaranteed to not contain null or blank values.
     * This means that disabled metrics have been filtered out.
     */
    @Singular
    private Map<String, String> metrics;
}
