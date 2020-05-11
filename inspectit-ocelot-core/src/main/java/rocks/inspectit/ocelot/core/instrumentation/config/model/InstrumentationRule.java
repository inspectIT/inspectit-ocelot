package rocks.inspectit.ocelot.core.instrumentation.config.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.*;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.EventRecordingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.Collection;
import java.util.Collections;
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
     * The names of the included rules.
     * This set stores the names of the included rules instead of references in order to avoid issues
     * with hashCode() / equals() due to cyclic dependencies.
     */
    @Singular
    private Set<String> includedRuleNames;

    /**
     * Holds all actions executed directly before the ones in {@link #entryActions}.
     */
    @Singular
    private Collection<ActionCallConfig> preEntryActions;

    /**
     * The actions executed on the method entry.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private Collection<ActionCallConfig> entryActions;

    /**
     * Holds all actions executed directly after the ones in {@link #entryActions}.
     */
    @Singular
    private Collection<ActionCallConfig> postEntryActions;

    /**
     * Holds all actions executed directly before the ones in {@link #exitActions}.
     */
    @Singular
    private Collection<ActionCallConfig> preExitActions;

    /**
     * The actions executed on the method exit.
     * The order of the actions in the list does not matter, they are ordered automatically by the
     * {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHookGenerator}.
     */
    @Singular
    private Collection<ActionCallConfig> exitActions;

    /**
     * Holds all actions executed directly after the ones in {@link #exitActions}.
     */
    @Singular
    private Collection<ActionCallConfig> postExitActions;

    /**
     * Holds the metrics to record.
     * Is a multiset because the same metric can be recorded twice on a given method.
     */
    @Builder.Default
    private Multiset<MetricRecordingSettings> metrics = HashMultiset.create();

    /**
     * The tracing related settings.
     */
    @Builder.Default
    private RuleTracingSettings tracing = RuleTracingSettings.NO_TRACING_AND_ATTRIBUTES;

    /**
     * The events to be recorded.
     */
    @Builder.Default
    private Set<EventRecordingSettings> events = Collections.emptySet();
}
