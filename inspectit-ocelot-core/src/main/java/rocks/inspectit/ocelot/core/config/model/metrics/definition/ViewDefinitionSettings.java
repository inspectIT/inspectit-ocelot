package rocks.inspectit.ocelot.core.config.model.metrics.definition;

import com.google.common.collect.Ordering;
import lombok.*;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Defines a single OpenCensus view for a measure.
 * The name of the view is defined through the key in the map {@link MetricDefinitionSettings#getViews()}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ViewDefinitionSettings {

    @AllArgsConstructor
    public enum Aggregation {
        LAST_VALUE("last value"), SUM("sum"), COUNT("count"),
        /**
         * Corresponds to OpenCensus "Distribution" aggregation
         */
        HISTOGRAM("histogram");

        @Getter
        private String readableName;
    }

    @Builder.Default
    private boolean enabled = true;

    /**
     * Description of the view.
     * If this is null a description is generated based on the name of the measure, the unit and the aggregation.
     */
    private String description;

    /**
     * The aggregation to use for this view
     */
    @NotNull
    @Builder.Default
    private Aggregation aggregation = Aggregation.LAST_VALUE;

    /**
     * Only relevant if aggregation is "HISTOGRAM".
     * In this case this list defines the boundaries of the buckets in the histogram
     */
    @Singular
    private List<@NotNull Double> bucketBoundaries;

    /**
     * Defines if this view should by default include all common tags.
     * Individual tags can still be disabled via {@link #tags}.
     */
    @Builder.Default
    private boolean withCommonTags = true;


    @Singular
    private Map<@NotBlank String, @NotNull Boolean> tags;

    public ViewDefinitionSettings getCopyWithDefaultsPopulated(String viewName, String measureDescription, String unit) {
        val result = toBuilder();
        if (description == null) {
            result.description(aggregation.getReadableName() + " of " + measureDescription + " [" + unit + "]");
        }
        return result.build();
    }

    @AssertFalse(message = "When using HISTOGRAM aggregation you must specify the bucket-boundaries!")
    boolean isBucketBoundariesNotSpecifiedForHistogram() {
        return enabled && aggregation == Aggregation.HISTOGRAM && CollectionUtils.isEmpty(bucketBoundaries);
    }

    @AssertTrue(message = "When using HISTOGRAM the specified bucket-boundaries must be sorted in ascending order and must contain each value at most once!")
    boolean isBucketBoundariesSorted() {
        if (enabled && aggregation == Aggregation.HISTOGRAM && !CollectionUtils.isEmpty(bucketBoundaries)) {
            return Ordering.natural().isStrictlyOrdered(bucketBoundaries);
        }
        return true;
    }


}
