package rocks.inspectit.ocelot.core.instrumentation.config.model;

import lombok.*;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.List;
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
     * The actions executed on the method entry.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private List<DataProviderCallConfig> entryProviders;

    /**
     * The actions executed on the method exit.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private List<DataProviderCallConfig> exitProviders;

    /**
     * Maps metrics to the data keys or constants used as sources, see {@link InstrumentationRuleSettings#getMetrics()}.
     * However, this map is guaranteed to not contain null or blank values.
     * This means that disabled metrics have been filtered out.
     */
    @Singular
    private Map<String, String> metrics;

    /**
     * The tracing related settings.
     */
    @Builder.Default
    private RuleTracingSettings tracing = new RuleTracingSettings();
}
