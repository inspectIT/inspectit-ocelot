package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import java.util.Set;

/**
 * Data container for documentation of a single Metric definition's {@link MetricDefinitionSettings} in Config Documentation.
 */
@Getter
public class MetricDocs extends BaseDocs {

    /**
     * The unit the metric is recorded in, see {@link MetricDefinitionSettings#getUnit()}.
     */
    private final String unit;

    public MetricDocs(String name, String description, String unit, Set<String> files) {
        // MetricDefinitions do not contain the info for the since field, so it is left empty
        super(name, description, null, files);

        this.unit = unit;
    }
}
