package rocks.inspectit.ocelot.core.metrics.jmx;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Scope;
import io.opencensus.stats.Measure;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
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

import javax.management.ObjectName;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JmxMetricsRecorder extends AbstractPollingMetricsRecorder implements JmxScraper.MBeanReceiver {

    private static final String METRIC_NAME_PREFIX = "jvm/jmx/";

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
    JmxMetricsRecorder(Tagger tagger, MeasuresAndViewsManager measuresAndViewsManager, StatsRecorder statsRecorder, CommonTagsManager commonTagsManager) {
        super("metrics.jmx");
        this.tagger = tagger;
        this.measureManager = measuresAndViewsManager;
        this.recorder = statsRecorder;
        this.commonTags = commonTagsManager;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates the new jmx scraper instance on every call.
     */
    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        // create a new scraper
        // TODO is this called on every update of every jmx settings update?
        this.jmxScraper = createScraper(configuration.getMetrics().getJmx(), this);
        this.lowerCaseMetricName = configuration.getMetrics().getJmx().isLowerCaseMetricName();

        // call super to handle scheduling
        return super.doEnable(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void takeMeasurement(MetricsSettings config) {
        try (Scope commonTagScope = commonTags.withCommonTagScope()) {
            jmxScraper.doScrape();
        } catch (Exception e) {
            // TODO
            log.error("error scraping", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getJmx().getFrequency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getJmx().isEnabled();
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
                        .collect(Collectors.toMap(k -> k, k -> true));

                return registerMeasure(metricName, attrDescription, tags);
            });

            TagContextBuilder tagContextBuilder = tagger.currentBuilder();
            beanProperties.entrySet().stream()
                    .skip(1)
                    .forEach(entry -> tagContextBuilder.putLocal(TagKey.create(entry.getKey()), TagValue.create(entry.getValue())));

            MeasureMap measureMap = recorder.newMeasureMap();
            measureMap.put(measure, metricValue);
            measureMap.record(tagContextBuilder.build());
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

        // this is a way to whitelist all
        List<ObjectName> whitelistAllObjectNames = new LinkedList<ObjectName>();
        whitelistAllObjectNames.add(null);

        if (null == objectNames || objectNames.size() == 0) {
            // if there is no entries, then collect everything
            // for this we need one null entry in the whitelist objects
            return new JmxScraper(whitelistAllObjectNames, Collections.emptyList(), receiver, jmx.isForcePlatformServer());
        } else {
            // go through map and collect what should be in which list
            List<ObjectName> whitelistedObjectNames = new ArrayList<>();
            List<ObjectName> blacklistedObjectNames = new ArrayList<>();
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

            // if we have no single whitelist object, then settings include only blacklisting
            // include all then as the whitelist
            return new JmxScraper(whitelistedObjectNames.size() > 0 ? whitelistedObjectNames : whitelistAllObjectNames, blacklistedObjectNames, receiver, jmx.isForcePlatformServer());
        }
    }

}
