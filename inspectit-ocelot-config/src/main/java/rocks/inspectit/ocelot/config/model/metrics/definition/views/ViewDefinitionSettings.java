package rocks.inspectit.ocelot.config.model.metrics.definition.views;

import io.opentelemetry.sdk.metrics.internal.view.Base2ExponentialHistogramAggregation;
import lombok.*;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import javax.validation.constraints.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.sdk.metrics.internal.aggregator.ExplicitBucketHistogramUtils.DEFAULT_HISTOGRAM_BUCKET_BOUNDARIES;

/**
 * Defines a single OpenTelemetry view for a metric.
 * The name of the view is defined through the key in the map {@link MetricDefinitionSettings#getViews()}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ViewDefinitionSettings {

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
    private AggregationType aggregation = AggregationType.LAST_VALUE; // TODO We should use the OTel DefaultAggregation for the instrument instead

    /**
     * The maximum amount of unique combinations of attributes for this view.
     * OpenTelemetry uses a default value of 2000
     */
    @Builder.Default
    private Integer cardinalityLimit = 2000; // TODO Can we combine this somehow with TagGuard?

    /**
     * Only relevant if aggregation is "HISTOGRAM".
     * In this case this list defines the boundaries of the buckets in the histogram
     */
    @Builder.Default
    private List<@NotNull Double> bucketBoundaries = DEFAULT_HISTOGRAM_BUCKET_BOUNDARIES;

    /**
     * Only relevant if aggregation is "EXPONENTIAL_HISTOGRAM".
     * In this case this defines the max number of positive buckets and negative buckets
     * (max total buckets is 2 * maxBuckets + 1 zero bucket).
     * Default from {@link Base2ExponentialHistogramAggregation}.
     */
    @Builder.Default
    private Integer maxBuckets = 160;

    /**
     * Only relevant if aggregation is "EXPONENTIAL_HISTOGRAM".
     * In this case this defines the maximum and initial scale.
     * Default from {@link Base2ExponentialHistogramAggregation}.
     */
    @Builder.Default
    private Integer maxScale = 20;

    /**
     * In case the view is a QUANTILES view, this set defines which quantiles shall be captured.
     * 0 corresponds to the minimum, 1 to the maximum.
     */
    @Builder.Default
    private List<@NotNull Double> quantiles = Arrays.asList(0.0, 0.5, 0.9, 0.95, 0.99, 1.0);

    /**
     * In case the view is a SMOOTHED_AVERAGE, this value (in percentage in the range (0,1)) defines,
     * how many metrics in the upper range shall be dropped.
     */
    @DecimalMax("1.0")
    @DecimalMin("0.0")
    @Builder.Default
    private Double dropUpper = 0.0;

    /**
     * In case the view is a SMOOTHED_AVERAGE, this value (in percentage in the range (0,1)) defines,
     * how many metrics in the lower range shall be dropped.
     */
    @DecimalMax("1.0")
    @DecimalMin("0.0")
    @Builder.Default
    private Double dropLower = 0.0;

    /**
     * The time window to use for windowed metrics.
     */
    @DurationMin(millis = 1L)
    @Builder.Default
    private Duration timeWindow = Duration.ofSeconds(15);

    /**
     * The maximum number of points to be buffered by this View.
     * Currently only relevant if the aggregation is QUANTILES or SMOOTHED_AVERAGE.
     * <p>
     * If this number is exceeded, a warning will be printed and points will be rejected until space is free again.
     */
    @Min(1)
    @Builder.Default
    private int maxBufferedPoints = 16384;

    /**
     * Defines if this view should by default include all common attributes.
     * Individual attributes still can be disabled via {@link #attributes}.
     */
    @Builder.Default
    private boolean withCommonAttributes = true;
    
    /**
     * Specifies which attributes should be used for this view.
     */
    @Singular
    private Map<@NotBlank String, @NotNull Boolean> attributes;

    public ViewDefinitionSettings getCopyWithDefaultsPopulated(String measureDescription, String unit) {
        val result = toBuilder();
        if (description == null) {
            result.description(aggregation.getReadableName() + " of " + measureDescription + " [" + unit + "]");
        }
        return result.build();
    }

    @AssertFalse(message = "When using QUANTILES aggregation you must specify the quantiles to use!")
    boolean isQuantilesNotSpecifiedForQuantileType() {
        return enabled && aggregation == AggregationType.QUANTILES && CollectionUtils.isEmpty(quantiles);
    }

    @AssertFalse(message = "When using HISTOGRAM aggregation you must specify the bucket-boundaries!")
    boolean isBucketBoundariesNotSpecifiedForHistogram() {
        return enabled && aggregation == AggregationType.HISTOGRAM && CollectionUtils.isEmpty(bucketBoundaries);
    }

    @AssertTrue(message = "When using HISTOGRAM the specified bucket-boundaries must be sorted in ascending order and must contain each value at most once!")
    boolean isBucketBoundariesSorted() {
        if (enabled && aggregation == AggregationType.HISTOGRAM && !CollectionUtils.isEmpty(bucketBoundaries)) {
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

    @AssertTrue(message = "The quantiles must be in the range [0,1]")
    boolean isQuantilesInRange() {
        return !enabled || aggregation != AggregationType.QUANTILES || quantiles.stream().noneMatch(q -> q < 0 || q > 1);
    }
}
