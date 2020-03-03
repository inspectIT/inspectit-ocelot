package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class MetricRecordingSettings {

    /**
     * The name of the metric to record. If this is null, the key of this instance from {@link InstrumentationRuleSettings#getMetrics()} is used as metric name.
     */
    private String metric;

    /**
     * The value to record for the given metric.
     * <p>
     * If the specified value can be parsed using @{@link Double#parseDouble(String)}, the given value is used as constant
     * measurement value for every time a method matching this rule is executed.
     * <p>
     * If the provided value is not parseable as a double, it is assumed that is a data key.
     * In this case the value in the context for the data key is used as value for the given measure.
     * For this reason the value present in the inspectit context for the given data key has to be an instance of {@link Number}.
     * <p>
     * This value in this map can also be null or an empty string, in this case simply no measurement is recorded.
     */
    private String value;

    /**
     * Constant tag key and values to add to the this metric when recorded. Constant tags will be overwritten by the
     * {@link #dataTags} if they share same key and data tag can be resolved.
     */
    @Builder.Default
    @NotNull
    private Map<String, String> constantTags = Collections.emptyMap();

    /**
     * Data tag key and values to add to the this metric when recorded. Actual data value is resolved from the active
     * inspectIT context.
     */
    @Builder.Default
    @NotNull
    private Map<String, String> dataTags = Collections.emptyMap();


    public MetricRecordingSettings copyWithDefaultMetricName(String defaultMetricName) {
        String metricName = getMetricNameOrDefault(defaultMetricName);
        return toBuilder().metric(metricName)
                .constantTags(Collections.unmodifiableMap(constantTags))
                .dataTags(Collections.unmodifiableMap(dataTags))
                .build();
    }

    /**
     * Checks if the metric to record exists.
     *
     * @param defaultMetricName in case {@link #metric} is null, this value is used as metric name
     * @param definedMetrics    the set of known valid metric names
     * @param vios              the violation builder to output errors
     */
    public void performValidation(String defaultMetricName, Set<String> definedMetrics, ViolationBuilder vios) {
        String name = getMetricNameOrDefault(defaultMetricName);
        if (!definedMetrics.contains(name)) {
            vios.message("Metric '{metric}' is not defined in inspectit.metrics.definitions!")
                    .parameter("metric", name)
                    .buildAndPublish();
        }
    }

    private String getMetricNameOrDefault(String defaultMetricName) {
        return StringUtils.isEmpty(metric) ? defaultMetricName : metric;
    }

}
