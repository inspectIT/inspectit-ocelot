package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.validation.AdditionalValidations;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Settings to limit the amount of recorded attribute values.
 * Let's keep the name 'TagGuard', since 'AttributeGuard' doesn't sound smooth
 */
@Data
@NoArgsConstructor
@AdditionalValidations
public class TagGuardSettings {

    private boolean enabled;

    /**
     * The schedule delay for the blocking task of the {@code MetricTagValueGuard}
     */
    private Duration scheduleDelay;

    /**
     * File, which contains metrics with their particular recorded attributes and their values
     */
    private String databaseFile;

    /**
     * String, which should be used as attribute value, if the defined limit of values is exceeded
     */
    private String overflowReplacement;

    /**
     * Default max values per attribute for all metrics that are not specified in {@link #maxValuesPerAttributeByMetric}
     * or {@link MetricDefinitionSettings#maxValuesPerAttribute}.
     */
    private int maxValuesPerAttribute;

    /**
     * Map containing max values per attribute by metric, e.g., {{'method_duration': 1337}}
     * <br>
     * max-values-per-attribute-by-metric: <br>
     *  method_duration: 1337 <br>
     *  http_in_responestime: 2000
     */
    @NotNull
    private Map<String, Integer> maxValuesPerAttributeByMetric = Collections.emptyMap();

}
