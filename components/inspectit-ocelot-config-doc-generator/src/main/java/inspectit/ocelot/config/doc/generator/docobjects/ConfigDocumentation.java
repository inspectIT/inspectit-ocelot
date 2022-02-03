package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

import java.util.List;

@Data
public class ConfigDocumentation {

    private final List<ScopeDoc> scopes;
    private final List<ActionDoc> actions;
    private final List<RuleDoc> rules;
    private final List<MetricDoc> metrics;

}
