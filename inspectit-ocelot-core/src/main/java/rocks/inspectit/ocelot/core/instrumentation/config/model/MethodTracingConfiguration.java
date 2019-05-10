package rocks.inspectit.ocelot.core.instrumentation.config.model;

import io.opencensus.trace.Span;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.Map;

@Value
@Builder
public class MethodTracingConfiguration {

    public static MethodTracingConfiguration NO_TRACING_AND_ATTRIBUTES =
            MethodTracingConfiguration.builder().startSpan(false).build();

    /**
     * See {@link RuleTracingSettings#isStartSpan()}.
     */
    private boolean startSpan;

    /**
     * See {@link RuleTracingSettings#getName()}.
     */
    private String spanNameDataKey;

    /**
     * See {@link RuleTracingSettings#getKind()}.
     */
    private Span.Kind spanKind;

    /**
     * See {@link RuleTracingSettings#getAttributes()}.
     */
    @Singular
    private Map<String, String> attributes;

    /**
     * Defines the dynamic conditions which are evaluated prior to starting a span.
     */
    private ConditionalActionSettings startSpanConditions;

    /**
     * Defines the dynamic conditions which are evaluated prior to writing span attributes.
     */
    private ConditionalActionSettings attributeConditions;
}
