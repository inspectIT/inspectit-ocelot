package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;

import java.util.Map;

@Getter
public class RuleMetricsDoc {

    public RuleMetricsDoc(String name, String value, Map<String, String> dataTags, Map<String, String> constantTags) {
        this.name = name;
        this.value = value;
        this.dataTags = dataTags;
        this.constantTags = constantTags;
    }

    String name;
    String value;
    Map<String, String> dataTags;
    Map<String, String> constantTags;

}
