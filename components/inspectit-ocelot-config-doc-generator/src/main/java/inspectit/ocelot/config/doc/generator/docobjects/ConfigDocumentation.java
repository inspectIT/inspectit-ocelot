package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

import java.util.List;

@Data
public class ConfigDocumentation {

    public ConfigDocumentation(List<ScopeDoc> scopes, List<ActionDoc> actions, List<RuleDoc> rules, List<MetricDoc> metrics) {
        this.scopes = scopes;
        this.actions = actions;
        this.rules = rules;
        this.metrics = metrics;
    }

    private List<ScopeDoc> scopes;
    private List<ActionDoc> actions;
    private List<RuleDoc> rules;
    private List<MetricDoc> metrics;

}
