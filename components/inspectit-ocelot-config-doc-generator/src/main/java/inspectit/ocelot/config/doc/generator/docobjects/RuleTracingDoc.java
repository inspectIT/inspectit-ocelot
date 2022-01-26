package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;

import java.util.Map;

@Getter
public class RuleTracingDoc {

    public RuleTracingDoc(Boolean startSpan, Map<String, String> startSpanConditions, Map<String, String> attributes) {
        this.startSpan = startSpan;
        this.startSpanConditions = startSpanConditions;
        this.attributes = attributes;
    }

    Boolean startSpan;
    Map<String, String> startSpanConditions;
    Map<String, String> attributes;

}
