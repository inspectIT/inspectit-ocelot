package rocks.inspectit.ocelot.core.metrics.jmx;

import static java.lang.Boolean.TRUE;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.jmx.JmxMetricsRecorderSettings;
import rocks.inspectit.ocelot.core.metrics.InstrumentManager;
import rocks.inspectit.ocelot.core.metrics.system.AbstractPollingMetricsRecorder;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import javax.management.ObjectName;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Recorder for the values exposed by the JMX beans. Recorder is using a scarper based on the prometheus jmx_exporter,
 * however this scraper supports multiple platforms servers as source.
 */
@Service
@Slf4j
public class JmxMetricsRecorder extends AbstractPollingMetricsRecorder implements JmxScraper.MBeanReceiver {

    /**
     * Prefix for all metrics exposed by this recorder.
     */
    private static final String METRIC_NAME_PREFIX = "jvm_jmx_";

    /**
     * Separator used to construct metric names.
     *
     * @see #metricName(String, LinkedHashMap, LinkedList, String)
     */
    private static final char METRIC_SEPARATOR = '_';

    /**
     * Scraper of the MBean objects.
     */
    private JmxScraper jmxScraper;

    /**
     * If metric name is lower-case.
     */
    private boolean lowerCaseMetricName;

    @Autowired
    public JmxMetricsRecorder() {
        super("metrics.jmx");
    }

    @VisibleForTesting
    JmxMetricsRecorder(InstrumentManager instrumentManager, CommonAttributesManager commonAttributes) {
        super("metrics.jmx");
        this.instrumentManager = instrumentManager;
        this.commonAttributes = commonAttributes;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates the new jmx scraper instance on every call.
     */
    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        // create a new scraper, called on every update of every jmx setting
        jmxScraper = createScraper(configuration.getMetrics().getJmx(), this);
        lowerCaseMetricName = configuration.getMetrics().getJmx().isLowerCaseMetricName();

        // call super to handle scheduling
        return super.doEnable(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void takeMetric(MetricsSettings metricsSettings) {
        try (Scope scope = commonAttributes.withCommonAttributesScope()) {
            jmxScraper.doScrape();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Duration getFrequency(MetricsSettings metricsSettings) {
        return metricsSettings.getJmx().getFrequency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkEnabledForConfig(MetricsSettings metricsSettings) {
        return metricsSettings.getJmx().isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordBean(String domain, LinkedHashMap<String, String> beanProperties, LinkedList<String> attrKeys, String attrName, String attrType, String attrDescription, Object value) {
        // get the metric value first, if we have no value here skip
        metricValue(value).ifPresent(metricValue -> {
            String metricName = metricName(domain, beanProperties, attrKeys, attrName);

            if (!instrumentManager.isInstrumentRegistered(metricName)) {
                Map<String, Boolean> attributes = beanProperties.keySet().stream()
                        .skip(1)
                        .collect(Collectors.toMap(Function.identity(), k -> true));

                registerMetric(metricName, attrDescription, attributes);
            }

            BaggageBuilder builder = Baggage.current().toBuilder();
            beanProperties.entrySet().stream()
                    .skip(1)
                    .forEach(entry ->
                            builder.put(entry.getKey(), AttributeUtils.resolveValue(entry.getKey(), entry.getValue()))
                    );

            instrumentManager.tryRecordingMetric(metricName, metricValue, builder.build());
        });
    }

    private void registerMetric(String metricName, String attrDescription, Map<String, Boolean> attributes) {
        // TODO better description here, include the FQN as well?
        MetricDefinitionSettings definitionSettingsWithLastValueView = MetricDefinitionSettings.builder()
                .description(attrDescription)
                .unit("na")
                .view(metricName, ViewDefinitionSettings.builder().attributes(attributes).build())
                .build()
                .getCopyWithDefaultsPopulated(metricName);

        Map<String, MetricDefinitionSettings> metric = Collections.singletonMap(metricName, definitionSettingsWithLastValueView);
        instrumentManager.processInstrumentUpdates(metric);
    }

    /**
     * Resolves the metric value, only Numbers and booleans are returned as double. Negative values resolve as empty as OC does not support them.
     *
     * @param value jxm value
     *
     * @return Double value or empty if jmx value can not be converted to a non-negative number.
     */
    private Optional<Double> metricValue(Object value) {
        if (value instanceof Number) {
            return Optional.of(((Number) value).doubleValue()).filter(d -> d >= 0d);
        } else if (value instanceof Boolean) {
            return Optional.of(TRUE.equals(value) ? 1d : 0d);
        } else {
            return Optional.empty();
        }
    }

    private String metricName(String domain, LinkedHashMap<String, String> beanProperties, LinkedList<String> attrKeys, String attrName) {
        StringBuilder stringBuilder = new StringBuilder(METRIC_NAME_PREFIX);
        stringBuilder.append(domain.replace('.', METRIC_SEPARATOR));

        if (beanProperties != null && !beanProperties.isEmpty()) {
            stringBuilder.append(METRIC_SEPARATOR);
            stringBuilder.append(beanProperties.values().iterator().next());
        }

        attrKeys.forEach(key -> {
            stringBuilder.append(METRIC_SEPARATOR);
            stringBuilder.append(key);
        });

        stringBuilder.append(METRIC_SEPARATOR);
        stringBuilder.append(attrName);

        String result = stringBuilder.toString();
        String cleanedResult = result.replace(' ', METRIC_SEPARATOR).replace("'", "");

        if (lowerCaseMetricName) {
            return cleanedResult.toLowerCase();
        } else {
            return cleanedResult;
        }
    }

    /**
     * Creates {@link JmxScraper} based on the configuration settings.
     */
    @VisibleForTesting
    static JmxScraper createScraper(JmxMetricsRecorderSettings jmx, JmxScraper.MBeanReceiver receiver) {
        Map<String, Boolean> objectNames = jmx.getObjectNames();

        List<ObjectName> whitelistedObjectNames = new ArrayList<>();
        List<ObjectName> blacklistedObjectNames = new ArrayList<>();

        if (null != objectNames) {
            // go through map and collect what should be in which list
            objectNames.forEach((objectNameRepresentation, whitelisted) -> {
                try {
                    ObjectName objectName = new ObjectName(objectNameRepresentation);
                    if (TRUE.equals(whitelisted)) {
                        whitelistedObjectNames.add(objectName);
                    } else {
                        blacklistedObjectNames.add(objectName);
                    }
                } catch (Exception e) {
                    log.warn("Error creating the object name from the configuration entry {}", objectNameRepresentation, e);
                }
            });
        }
        return new JmxScraper(whitelistedObjectNames, blacklistedObjectNames, receiver, jmx.isForcePlatformServer());
    }
}
