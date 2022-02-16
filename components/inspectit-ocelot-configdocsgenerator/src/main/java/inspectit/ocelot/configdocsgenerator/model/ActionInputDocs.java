package inspectit.ocelot.configdocsgenerator.model;

import lombok.Data;

/**
 * Data container for documentation of a single Action's inputs in Config Documentation.
 */
@Data
public class ActionInputDocs {

    /**
     * Name of the input parameter.
     */
    private final String name;

    /**
     * Type of the input parameter.
     */
    private final String type;

    /**
     * Description of the input parameter.
     */
    private final String description;

    public ActionInputDocs(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }
}
