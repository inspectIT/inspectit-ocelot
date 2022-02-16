package inspectit.ocelot.configdocsgenerator.model;

import lombok.Data;

import java.util.List;

@Data
public class ConfigDocumentation {

    private final List<BaseDocs> scopes;

    private final List<ActionDocs> actions;

    private final List<RuleDocs> rules;

    private final List<MetricDocs> metrics;

}
