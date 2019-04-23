package rocks.inspectit.ocelot.core.config.model.instrumentation.rules;

import io.opencensus.trace.Span;
import lombok.*;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ConditionalActionSettings;

import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class RuleTracingSettings extends ConditionalActionSettings {

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
     * Can be null, in which case it is just a "normal" span.
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
}
