package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import io.opencensus.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleTracingSettings {

    public static final RuleTracingSettings NO_TRACING_AND_ATTRIBUTES = RuleTracingSettings.builder()
            .startSpan(false)
            .endSpan(false)
            .build();

    /**
     * If true, the methods which are matched by the rule containing this settings will appear in traces.
     * Only happens, if no span is continued as configured by {@link #continueSpan}
     * If this is null, this means that the containing rule does not care whether a span is started or not.
     */
    private Boolean startSpan;

    /**
     * If true, the span either started or continued will be ended as soon as the instrumented method returns.
     * If this is null, this means that the containing rule does not care whether a span is ended or not.
     * If any rule for a method specified that a span is started or continued but none specified a value for
     * "endSpan", the span is automatically ended.
     */
    private Boolean endSpan;

    /**
     * If not null, this rule will attempt to continue the span stored under the given data key.
     * This only happens if the {@link #continueSpanConditions} are met and the the value for the given data key is a valid Span.
     * If this is null, this means that the containing rule does not care whether a span is started or not.
     */
    private String continueSpan;

    /**
     * If not null, the span started or continued by this rule will be stored under the given data key.
     */
    private String storeSpan;

    /**
     * Specifies a data key to use as span name from the {@link rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext}.
     * If this is null or the value assigned to the data key is null, the FQN of the method will be used as name for the span.
     */
    private String name;

    /**
     * If this value is a numeric value, it is interpreted as a fixed probability deciding with which probability a trace is started,
     * in case no active trace exists, start-span is true, all start-span conditions are met.
     * <p>
     * Alternatively, this value is interpreted as a data key. The value of the given data key is extracted and used as probability.
     * This for example allows to define different probabilities, e.g. based on the HTTP path.
     * <p>
     * If this is null, it defaults to {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings#sampleProbability}
     */
    private String sampleProbability;

    /**
     * The kind of the span, e.g. SERVER or CLIENT.
     * Can be null, in which case it is a span of unspecified kind.
     */
    private Span.Kind kind;

    /**
     * Maps names of span attributes to data keys.
     * After all method-exit actions have been executed on a method hook,
     * the data keys specified by this map are read and stored at the current span as attributes.
     * If the data key assigned to an attribute is null or empty, no assignment is performed.
     * <p>
     * Note that attributes are recorded independently of whether the current span was started by this span (e.g. if start-span is true).
     * However, all defined conditions via the members inherited from {@link ConditionalActionSettings} are respected.
     */
    @Builder.Default
    private Map<@NotBlank String, String> attributes = Collections.emptyMap();

    /**
     * Defines conditions which make the start-span flag conditional.
     */
    @Builder.Default
    @Valid
    @NotNull
    ConditionalActionSettings startSpanConditions = new ConditionalActionSettings();

    /**
     * Defines conditions which make the end-span flag conditional.
     */
    @Builder.Default
    @Valid
    @NotNull
    ConditionalActionSettings endSpanConditions = new ConditionalActionSettings();

    /**
     * Defines conditions which make the continue-span flag conditional.
     */
    @Builder.Default
    @Valid
    @NotNull
    ConditionalActionSettings continueSpanConditions = new ConditionalActionSettings();

    /**
     * Defines conditions which make the attribute definitions conditional.
     */
    @Builder.Default
    @Valid
    @NotNull
    ConditionalActionSettings attributeConditions = new ConditionalActionSettings();
}
