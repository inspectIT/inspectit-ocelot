package rocks.inspectit.ocelot.core.instrumentation.config.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;

import java.util.List;

/**
 * The configuration used to build a {@link MethodHook}
 * Note that the {@link #equals(Object)} function on this class is used to decide whether a recreation of the hook is required.
 */
@Builder
@Value
public class MethodHookConfiguration {

    /**
     * The combined tracing settings from all rules matching this method.
     */
    @Builder.Default
    private RuleTracingSettings tracing = RuleTracingSettings.NO_TRACING_AND_ATTRIBUTES;

    /**
     * Holds all actions executed in the specified order directly before the ones in {@link #entryActions}.
     */
    @Singular
    private List<ActionCallConfig> preEntryActions;

    /**
     * The ordered list of data assignments performed on method entry.
     */
    @Singular
    private List<ActionCallConfig> entryActions;

    /**
     * Holds all actions executed in the specified order directly after the ones in {@link #entryActions}.
     * These actions are executed after a span has been started or continued, if requested.
     */
    @Singular
    private List<ActionCallConfig> postEntryActions;

    /**
     * Holds all actions executed in the specified order directly before the ones in {@link #exitActions}.
     */
    @Singular
    private List<ActionCallConfig> preExitActions;

    /**
     * The ordered list of data assignments performed on method exit.
     */
    @Singular
    private List<ActionCallConfig> exitActions;

    /**
     * Holds all actions executed in the specified order directly after the ones in {@link #entryActions}.
     * These actions are executed after a span has been finished, if requested.
     */
    @Singular
    private List<ActionCallConfig> postExitActions;

    /**
     * The metrics to record.
     * Is a {@link Multiset} instead of a set because different rules can have exactly the same {@link MetricRecordingSettings}
     * definition but are applied on the same method.
     * This should lead to the same metric being written twice.
     */
    @Builder.Default
    private Multiset<MetricRecordingSettings> metrics = HashMultiset.create();

    @Builder.Default
    private boolean traceEntryHook = false;

    @Builder.Default
    private boolean traceExitHook = false;
}
