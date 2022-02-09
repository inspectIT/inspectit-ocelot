package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.*;

@Getter
@Setter
public class RuleActionCallDoc {

    public RuleActionCallDoc(String name, String action) {
        this.name = name;
        this.action = action;
    }

    public RuleActionCallDoc(RuleActionCallDoc fromIncludedRule, String inheritedFrom) {
        this.name = fromIncludedRule.getName();
        this.action = fromIncludedRule.getAction();
        this.inheritedFrom = inheritedFrom;
    }

    private final String name;
    private final String action;
    private String inheritedFrom;

}
