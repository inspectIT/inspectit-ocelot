package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

/**
 * Data container for documentation of a single Metric definition's {@link MetricDefinitionSettings} in Config Documentation.
 */
@Getter
public class MetricDocs extends BaseDocs {

    /**
     * The unit the metric is recorded in, see {@link MetricDefinitionSettings#getUnit()}.
     */
    private final String unit;

    public MetricDocs(String name, String description, String unit) {
        // MetricDefinitions do not contain the info for the since field, so it is left empty
        super(name, description, null);

        this.unit = unit;
    }
}