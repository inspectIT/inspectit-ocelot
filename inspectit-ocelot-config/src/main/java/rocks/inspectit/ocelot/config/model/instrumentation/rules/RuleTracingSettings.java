package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import io.opencensus.trace.Span;
import lombok.*;
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

    public static final RuleTracingSettings NO_TRACING_AND_ATTRIBUTES = new RuleTracingSettings();

    /**
     * If true, the methods which are matched by the rule containing this settings will appear in traces.
     */
    @Builder.Default
    private boolean startSpan = false;

    /**
     * Specifies a data key to use as span name from the {@link rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext}.
     * If this is null or the value assigned to the data key is null, the FQN of the method will be used as name for the span.
     */
    private String name;

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
     * Defines conditions which make the attribute definitions conditional.
     */
    @Builder.Default
    @Valid
    @NotNull
    ConditionalActionSettings attributeConditions = new ConditionalActionSettings();
}
