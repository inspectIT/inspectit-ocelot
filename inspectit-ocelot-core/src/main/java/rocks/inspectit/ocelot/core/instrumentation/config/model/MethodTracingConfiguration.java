package rocks.inspectit.ocelot.core.instrumentation.config.model;

import io.opencensus.trace.Span;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.Map;

@Value
@Builder
public class MethodTracingConfiguration {

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
     * Defines a dynamic condition which can be used to decide at runtime if a given trace should be recorded or not.
     * In addition the conditions define if attributes will be written or not.
     */
    private ConditionalActionSettings conditions;
}
