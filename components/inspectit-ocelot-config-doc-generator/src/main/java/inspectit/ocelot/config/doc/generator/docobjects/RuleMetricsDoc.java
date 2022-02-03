package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

import java.util.Map;

@Data
public class RuleMetricsDoc {

    private final String name;
    private final String value;
    private final Map<String, String> dataTags;
    private final Map<String, String> constantTags;

}
