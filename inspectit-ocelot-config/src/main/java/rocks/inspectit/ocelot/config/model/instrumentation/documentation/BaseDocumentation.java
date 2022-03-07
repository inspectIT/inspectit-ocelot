package rocks.inspectit.ocelot.config.model.instrumentation.documentation;

import lombok.Data;

/**
 * Data container for information to be used to generate a configuration documentation.
 */
@Data
public class BaseDocumentation {

    /**
     * A description of what the documented object, e.g. a scope, action or rule, does.
     */
    private String description = "";

    /**
     * Since when the documented object is part of the configuration.
     */
    private String since = "";
}