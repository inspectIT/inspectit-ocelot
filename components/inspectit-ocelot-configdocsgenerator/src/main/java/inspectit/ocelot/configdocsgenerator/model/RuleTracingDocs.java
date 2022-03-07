package inspectit.ocelot.configdocsgenerator.model;

import lombok.Data;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;

import java.util.Map;

/**
 * Data container for documentation of a single Rule's {@link RuleTracingSettings} in Config Documentation.
 */
@Data
public class RuleTracingDocs {

    /**
     * See {@link RuleTracingSettings#getStartSpan()}
     */
    private final Boolean startSpan;

    /**
     * See {@link RuleTracingSettings#getStartSpanConditions()} ()}
     */
    private final Map<String, String> startSpanConditions;

    /**
     * See {@link RuleTracingSettings#getAttributes()} ()}
     */
    private final Map<String, String> attributes;

}
