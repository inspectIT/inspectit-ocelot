package inspectit.ocelot.configdocsgenerator.model;

import lombok.Data;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

import java.util.Map;

/**
 * Data container for documentation of a single Rule's {@link MetricRecordingSettings} in Config Documentation.
 */
@Data
public class RuleMetricsDocs {

    /**
     * Name of the metric, see {@link MetricRecordingSettings#getMetric()}.
     */
    private final String name;

    /**
     * See {@link MetricRecordingSettings#getValue()}.
     */
    private final String value;

    /**
     * See {@link MetricRecordingSettings#getDataTags()}.
     */
    private final Map<String, String> dataTags;

    /**
     * See {@link MetricRecordingSettings#getConstantTags()}.
     */
    private final Map<String, String> constantTags;

}
