package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;

import java.util.List;

@Getter
public class ActionDoc extends BaseDoc {

    public ActionDoc(String name, String description, List<ActionInputDoc> inputs, String returnDescription, Boolean isVoid) {
        super(name, description);
        this.inputs = inputs;
        this.returnDescription = returnDescription;
        this.isVoid = isVoid;
    }

    List<ActionInputDoc> inputs;
    String returnDescription;
    Boolean isVoid;

}
