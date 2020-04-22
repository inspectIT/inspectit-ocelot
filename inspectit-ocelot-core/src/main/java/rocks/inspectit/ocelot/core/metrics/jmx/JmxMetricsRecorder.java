package rocks.inspectit.ocelot.core.metrics.jmx;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Scope;
import io.opencensus.stats.Measure;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tagger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.jmx.JmxMetricsRecorderSettings;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.metrics.system.AbstractPollingMetricsRecorder;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;
import rocks.inspectit.ocelot.core.tags.TagUtils;

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
    private static final String METRIC_NAME_PREFIX = "jvm/jmx/";

    /**
     * Separator used to construct metric names.
     *
     * @see #metricName(String, LinkedHashMap, LinkedList, String)
     */
    private static final char METRIC_SEPARATOR = '/';

    /**
     * Tagger.
     */
    public final Tagger tagger;

    /**
     * Scraper of the MBean objects.
     */
    private JmxScraper jmxScraper;

    /**
     * If metric name is lower-case.
     */
    private boolean lowerCaseMetricName;

    @Autowired
    public JmxMetricsRecorder(Tagger tagger) {
        super("metrics.jmx");
        this.tagger = tagger;
    }

    @VisibleForTesting
    JmxMetricsRecorder(Tagger tagger, MeasuresAndViewsManager measuresAndViewsManager, CommonTagsManager commonTagsManager) {
        super("metrics.jmx");
        this.tagger = tagger;
        measureManager = measuresAndViewsManager;
        commonTags = commonTagsManager;
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
    protected void takeMeasurement(MetricsSettings metricsSettings) {
        try (Scope commonTagScope = commonTags.withCommonTagScope()) {
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
            Measure.MeasureDouble measure = measureManager.getMeasureDouble(metricName).orElseGet(() -> {
                Map<String, Boolean> tags = beanProperties.keySet().stream()
                        .skip(1)
                        .collect(Collectors.toMap(Function.identity(), k -> true));

                return registerMeasure(metricName, attrDescription, tags);
            });

            TagContextBuilder tagContextBuilder = tagger.currentBuilder();
            beanProperties.entrySet().stream()
                    .skip(1)
                    .forEach(entry -> tagContextBuilder.putLocal(TagKey.create(entry.getKey()), TagUtils.createTagValue(entry.getValue())));

            measureManager.tryRecordingMeasurement(measure.getName(), metricValue, tagContextBuilder.build());
        });
    }

    private Measure.MeasureDouble registerMeasure(String metricName, String attrDescription, Map<String, Boolean> tags) {
        // TODO better description here, include the FQN as well?
        MetricDefinitionSettings definitionSettingsWithLastValueView = MetricDefinitionSettings.builder()
                .description(attrDescription)
                .unit("na")
                .view(metricName, ViewDefinitionSettings.builder()
                        .tags(tags)
                        .build()
                )
                .build()
                .getCopyWithDefaultsPopulated(metricName);

        measureManager.addOrUpdateAndCacheMeasureWithViews(metricName, definitionSettingsWithLastValueView);
        return measureManager.getMeasureDouble(metricName).get();
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
            return Optional.of(((Number) value).doubleValue())
                    .filter(d -> d >= 0d);
        } else if (value instanceof Boolean) {
            return Optional.of(((Boolean) value) ? 1d : 0d);
        } else {
            return Optional.empty();
        }
    }

    private String metricName(String domain, LinkedHashMap<String, String> beanProperties, LinkedList<String> attrKeys, String attrName) {
        StringBuilder stringBuilder = new StringBuilder(METRIC_NAME_PREFIX);
        stringBuilder.append(domain.replace('.', METRIC_SEPARATOR));

        if (beanProperties != null && beanProperties.size() > 0) {
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

        if (lowerCaseMetricName) {
            return result.toLowerCase();
        } else {
            return result;
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
                    if (whitelisted) {
                        whitelistedObjectNames.add(objectName);
                    } else {
                        blacklistedObjectNames.add(objectName);
                    }
                } catch (Exception e) {
                    log.warn("Error creating the object name from the configuration entry {}.", objectNameRepresentation, e);
                }
            });
        }

        return new JmxScraper(whitelistedObjectNames, blacklistedObjectNames, receiver, jmx.isForcePlatformServer());
    }

}
