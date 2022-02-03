package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Data;

@Data
public class ActionInputDoc{

    public ActionInputDoc(String name, String type, String description){
        this.name = name;
        this.type = type;
        this.description = description;
    }

    private final String name;
    private final String type;
    private final String description;
}
