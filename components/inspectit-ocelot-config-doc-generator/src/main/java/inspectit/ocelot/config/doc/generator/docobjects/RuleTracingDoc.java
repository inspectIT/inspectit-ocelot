package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

import java.util.Map;

@Data
public class RuleTracingDoc {

    private final Boolean startSpan;
    private final Map<String, String> startSpanConditions;
    private final Map<String, String> attributes;

}
