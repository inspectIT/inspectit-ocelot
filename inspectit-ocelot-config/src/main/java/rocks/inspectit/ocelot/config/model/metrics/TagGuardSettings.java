package rocks.inspectit.ocelot.config.model.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.validation.AdditionalValidations;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AdditionalValidations
public class TagGuardSettings {

    private boolean enabled;

    /**
     * The schedule delay for the blocking task of the {@code MeasureTagValueGuard}
     */
    private Duration scheduleDelay;

    /**
     * File, which contains metrics with their particular recorded tags and their tag values
     */
    private String databaseFile;

    /**
     * String, which should be used as tag value, if the defined limit of tag values is exceeded
     */
    private String overflowReplacement;

    /**
     * Default max values per tag for all measures that are not specified in {@link #maxValuesPerTagByMeasure} or {@link rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings#maxValuesPerTag}.
     */
    private int maxValuesPerTag;

    /**
     * Map containing max values per tag by Measure, e.g., {{'method_duration': 1337}}
     * <br> max-values-per-tag-by-measure:
     *  - method_duration: 1337 <br>
     *  - http_in_responestime: 2000
     */
    @NotNull
    private Map<String, Integer> maxValuesPerTagByMeasure = Collections.emptyMap();

}
