package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode (callSuper = true)
public class ActionDoc extends BaseDoc {

    public ActionDoc(String name, String description, List<ActionInputDoc> inputs, String returnDescription, Boolean isVoid) {
        super(name, description);
        this.inputs = inputs;
        this.returnDescription = returnDescription;
        this.isVoid = isVoid;
    }

    private final List<ActionInputDoc> inputs;
    private final String returnDescription;
    private final Boolean isVoid;

}
