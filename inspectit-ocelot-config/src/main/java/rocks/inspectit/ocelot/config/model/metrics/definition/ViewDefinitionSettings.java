package rocks.inspectit.ocelot.config.model.metrics.definition;

import lombok.*;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.*;
import java.time.Duration;
import java.util.Arrays;
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
        LAST_VALUE("last value"), SUM("sum"), COUNT("count"), QUANTILES("quantiles"), SMOOTHED_AVERAGE("smoothed average"),
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
     * In case the view is a quantile view, this list defines which quantiles shall be captured.
     * 0 corresponds to the minimum, 1 to the maximum.
     */
    @Builder.Default
    private List<@NotNull Double> quantiles = Arrays.asList(0.0, 0.5, 0.9, 0.95, 0.99, 1.0);

    /**
     * In case the view is a smoothed_average, this value (in percentage in the range (0,1)) defines, how many metrics in the upper range shall be dropped.
     */
    @DecimalMax("1.0")
    @DecimalMin("0.0")
    @Builder.Default
    private Double dropUpper = 0.0;

    /**
     * In case the view is a smoothed_average, this value (in percentage in the range (0,1)) defines, how many metrics in the lower range shall be dropped.
     */
    @DecimalMax("1.0")
    @DecimalMin("0.0")
    @Builder.Default
    private Double dropLower = 0.0;

    /**
     * The time window to use for windowed metrics (currently only quantiles).
     * Can be null, in this case the default provided via {@link #getCopyWithDefaultsPopulated(String, String, Duration)}.
     * is used.
     */
    @DurationMin(millis = 1L)
    private Duration timeWindow;

    /**
     * The maximum number of points to be buffered by this View.
     * Currently only relevant if the aggregation is QUANTILES.
     * <p>
     * If this number is exceeded, a warning will be printed and points will be rejected until space is free again.
     */
    @Min(1)
    @Builder.Default
    private int maxBufferedPoints = 16384;

    /**
     * Defines if this view should by default include all common tags.
     * Individual tags can still be disabled via {@link #tags}.
     */
    @Builder.Default
    private boolean withCommonTags = true;
    
    /**
     * Specifies which tags should be used for this view.
     */
    @Singular
    private Map<@NotBlank String, @NotNull Boolean> tags;

    public ViewDefinitionSettings getCopyWithDefaultsPopulated(String measureDescription, String unit, Duration defaultTimeWindow) {
        val result = toBuilder();
        if (description == null) {
            result.description(aggregation.getReadableName() + " of " + measureDescription + " [" + unit + "]");
        }
        if (timeWindow == null) {
            if (defaultTimeWindow == null && aggregation == Aggregation.QUANTILES) {
                throw new IllegalArgumentException("A default time window must be provided for quantile views");
            }
            result.timeWindow(defaultTimeWindow);
        }
        return result.build();
    }

    @AssertFalse(message = "When using QUANTILES aggregation you must specify the quantiles to use!") boolean isQuantilesNotSpecifiedForercentileType() {
        return enabled && aggregation == Aggregation.QUANTILES && CollectionUtils.isEmpty(quantiles);
    }

    @AssertFalse(message = "When using HISTOGRAM aggregation you must specify the bucket-boundaries!") boolean isBucketBoundariesNotSpecifiedForHistogram() {
        return enabled && aggregation == Aggregation.HISTOGRAM && CollectionUtils.isEmpty(bucketBoundaries);
    }

    @AssertTrue(message = "When using HISTOGRAM the specified bucket-boundaries must be sorted in ascending order and must contain each value at most once!") boolean isBucketBoundariesSorted() {
        if (enabled && aggregation == Aggregation.HISTOGRAM && !CollectionUtils.isEmpty(bucketBoundaries)) {
            Double previous = null;
            for (double boundary : bucketBoundaries) {
                if (previous != null && previous >= boundary) {
                    return false;
                }
                previous = boundary;
            }
        }
        return true;
    }

    @AssertTrue(message = "The quantiles must be in the range [0,1]") boolean isQuantilesInRange() {
        return !enabled || aggregation != Aggregation.QUANTILES || quantiles.stream().noneMatch(q -> q < 0 || q > 1);
    }

}
