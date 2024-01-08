package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.BaseDocumentation;

import java.util.Set;

/**
 * Data container for documentation of a single object in Config Documentation, e.g. a scope or action.
 */
@RequiredArgsConstructor
@Getter
public class BaseDocs {

    /**
     * Name of the documented object.
     */
    private final String name;

    /**
     * See {@link BaseDocumentation#getDescription()}.
     */
    private final String description;

    /**
     * See {@link BaseDocumentation#getSince()}.
     */
    private final String since;

    /**
     * File paths, which contain definition for the current object
     */
    private final Set<String> files;

}
