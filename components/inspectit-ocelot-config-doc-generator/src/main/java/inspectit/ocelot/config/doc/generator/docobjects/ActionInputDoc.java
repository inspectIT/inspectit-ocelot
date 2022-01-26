package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;

@Getter
public class ActionInputDoc{

    public ActionInputDoc(String name, String type, String description){
        this.name = name;
        this.type = type;
        this.description = description;
    }

    String name;
    String type;
    String description;
}
