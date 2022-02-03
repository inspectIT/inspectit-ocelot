package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

@Data
public abstract class BaseDoc {

    public BaseDoc(String name, String description) {
        this.description = description;
        this.name = name;
    }

    private final String description;
    private final String name;

}
