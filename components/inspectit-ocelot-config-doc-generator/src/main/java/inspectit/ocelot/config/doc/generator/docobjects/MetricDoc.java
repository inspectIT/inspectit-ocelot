package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;

@Getter
public class MetricDoc extends BaseDoc {

    public MetricDoc(String name, String description, String unit){
        super(name, description);
        this.unit = unit;
    }

    String unit;
}
