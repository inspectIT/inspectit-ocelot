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

    /**
     * The schedule delay of the {@code TagValueGuard}
     */
    private Duration scheduleDelay;

    /**
     *
     */
    private String databaseFile;


    private String overflowReplacement;

    private boolean enabled;

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
