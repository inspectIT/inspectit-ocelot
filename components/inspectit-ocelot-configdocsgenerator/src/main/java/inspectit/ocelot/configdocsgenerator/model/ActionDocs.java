package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;

import java.util.List;

/**
 * Data container for documentation of a single Action's {@link GenericActionSettings} in Config Documentation.
 */
@Getter
public class ActionDocs extends BaseDocs {

    /**
     * List of documentations for inputs of the action.
     */
    private final List<ActionInputDocs> inputs;

    /**
     * Description of the action's return value.
     */
    private final String returnDescription;

    /**
     * Whether the action returns anything, see {@link GenericActionSettings#getIsVoid()}
     */
    private final Boolean isVoid;

    public ActionDocs(String name, String description, String since, List<ActionInputDocs> inputs, String returnDescription, Boolean isVoid) {
        super(name, description, since);
        this.inputs = inputs;
        this.returnDescription = returnDescription;
        this.isVoid = isVoid;
    }

}