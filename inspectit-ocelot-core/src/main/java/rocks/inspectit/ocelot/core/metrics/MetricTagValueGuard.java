package rocks.inspectit.ocelot.core.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.TagGuardSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.tagGuard.PersistedAttributesReaderWriter;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Allows to control the amount of recorded values for attributes.
 * Let's keep the name 'TagGuard', since 'AttributeGuard' doesn't sound smooth
 */
@Component
@Slf4j
public class MetricTagValueGuard {

    private static final String attributeOverFlowMessageTemplate = "Overflow in metric %s for attribute key %s";

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private AgentHealthManager agentHealthManager;

    /**
     * Common attributes manager needed for gathering common attributes when recording metrics.
     */
    @Autowired
    private CommonAttributesManager commonAttributes;

    @Autowired
    private ScheduledExecutorService executor;

    private PersistedAttributesReaderWriter fileReaderWriter;

    /**
     * Map of metric names and their related set of attribute keys, which are currently blocked.
     */
    private final Map<String, Set<String>> blockedAttributeKeysByMetric = Maps.newHashMap();

    private Set<AttributesHolder> latestAttributes = Collections.synchronizedSet(new HashSet<>());

    private volatile boolean isShuttingDown = false;

    private boolean hasValueOverflow = false;


    private Future<?> blockAttributeValuesFuture;

    @PostConstruct
    protected void init() {
        scheduleTagGuardJob();
        TagGuardSettings settings = env.getCurrentConfig().getMetrics().getTagGuard();
        log.info("TagValueGuard started with scheduleDelay {} and database file {}", settings.getScheduleDelay(), settings.getDatabaseFile());
    }

    private void scheduleTagGuardJob() {
        Duration tagGuardScheduleDelay = env.getCurrentConfig().getMetrics().getTagGuard().getScheduleDelay();
        blockAttributeValuesFuture = executor.schedule(blockAttributeValuesTask, tagGuardScheduleDelay.toNanos(), TimeUnit.NANOSECONDS);
    }


    @PreDestroy
    protected void stop() {
        if (isTagGuardDisabled()) {
            return;
        }

        isShuttingDown = true;
        blockAttributeValuesFuture.cancel(true);
    }

    /**
     * Gets the max value amount per attribute for the given measure by hierarchically extracting
     * {@link MetricDefinitionSettings#maxValuesPerAttribute} (prio 1),
     * {@link TagGuardSettings#maxValuesPerAttributeByMetric} (prio 2) and
     * {@link TagGuardSettings#maxValuesPerAttribute} (default).
     *
     * @param metricName the current metric
     *
     * @return The maximum amount of attribute values for the given metric
     */
    @VisibleForTesting
    int getMaxValuesPerAttribute(String metricName, InspectitConfig config) {
        int maxValuesPerAttribute = config.getMetrics().getDefinitions().get(metricName).getMaxValuesPerAttribute();

        if (maxValuesPerAttribute > 0) return maxValuesPerAttribute;

        Map<String, Integer> maxValuesPerAttributePerMetricMap = config.getMetrics()
                .getTagGuard()
                .getMaxValuesPerAttributeByMetric();
        return maxValuesPerAttributePerMetricMap.getOrDefault(metricName, config.getMetrics()
                .getTagGuard()
                .getMaxValuesPerAttribute());
    }

    /**
     * Creates the full baggage, including all specified attributes, for the current metric
     *
     * @param context        current context
     * @param metricAccessor accessor for the metric as well as the particular attributes
     *
     * @return the baggage including all attributes for the current metric
     */
    public Baggage getBaggage(IHookAction.ExecutionContext context, MetricAccessor metricAccessor) {
        Map<String, String> attributes = Maps.newHashMap();
        String metricName = metricAccessor.getName();
        InspectitContextImpl inspectitContext = context.getInspectitContext();
        TagGuardSettings tagGuardSettings = env.getCurrentConfig().getMetrics().getTagGuard();

        Set<String> blockedAttributeKeys = blockedAttributeKeysByMetric.getOrDefault(metricName, Sets.newHashSet());
        log.debug("Currently blocked attributes for metric {}, due to exceeding the configured value limit: {}",
                metricName, blockedAttributeKeys);

        // first common attributes to allow to overwrite by constant or data attributes
        commonAttributes.getCommonAttributeKeys().forEach(key ->
            Optional.ofNullable(inspectitContext.getData(key))
                    .ifPresent(value -> attributes.put(key, AttributeUtils.resolveValue(key, value)))
        );

        // then constant attributes to allow to overwrite by data
        metricAccessor.getConstantAttributes().forEach((key, value) -> {
            if (tagGuardSettings.isEnabled() && blockedAttributeKeys.contains(key)) {
                String overflowReplacement = env.getCurrentConfig().getMetrics().getTagGuard().getOverflowReplacement();
                attributes.put(key, overflowReplacement);
            } else {
                attributes.put(key, AttributeUtils.resolveValue(key, value));
            }
        });

        // go over data attributes and match the value to the key from the contextTags (if available)
        metricAccessor.getDataAttributeAccessors().forEach((key, accessor) -> {
            if (tagGuardSettings.isEnabled() && blockedAttributeKeys.contains(key)) {
                String overflowReplacement = env.getCurrentConfig().getMetrics().getTagGuard().getOverflowReplacement();
                attributes.put(key, overflowReplacement);
            } else {
                Optional.ofNullable(accessor.get(context))
                        .ifPresent(value -> attributes.put(key, AttributeUtils.resolveValue(key, value)));
            }
        });

        BaggageBuilder builder = Baggage.builder();
        attributes.forEach((key, value) -> builder.put(key, AttributeUtils.resolveValue(key, value)));

        // store the new attributes for this measure as simple object and delay traversing trough tagKeys to async job
        latestAttributes.add(new AttributesHolder(metricName, attributes));

        return builder.build();
    }

    private boolean isTagGuardDisabled() {
        return !env.getCurrentConfig().getMetrics().getTagGuard().isEnabled();
    }

    /**
     * Task, which reads the persisted attribute values to determine, which attributes should be blocked,
     * because of exceeding the specific value limit.
     * If new attribute values have been created, they will be persisted.
     */
    @VisibleForTesting
    Runnable blockAttributeValuesTask = () -> {
        if (isNotWritable()) {
            return;
        }

        Map<String, Map<String, Set<String>>> storedAttributes = fileReaderWriter.read();
        processNewAttributes(storedAttributes);
        fileReaderWriter.write(storedAttributes);
        removeBlockedAttributes(storedAttributes);

        // invalidate incident, if attribute overflow was detected, but no more attributes are blocked
        boolean noBlockedAttributeKeys = blockedAttributeKeysByMetric.values().stream().allMatch(Set::isEmpty);
        if (hasValueOverflow && noBlockedAttributeKeys) {
            agentHealthManager.invalidateIncident(this.getClass(), "Overflow for attributes resolved");
            hasValueOverflow = false;
        }

        if (!isShuttingDown) scheduleTagGuardJob();
    };

    private boolean isNotWritable() {
        if (isTagGuardDisabled()) {
            return true;
        }

        initAttributeReaderWriter();
        return Objects.isNull(fileReaderWriter);
    }

    private void initAttributeReaderWriter() {
        final String filename = getFilename();
        if (Objects.nonNull(filename)) {
            fileReaderWriter = PersistedAttributesReaderWriter.of(filename);
        }
    }

    private String getFilename() {
        TagGuardSettings tagGuardSettings = env.getCurrentConfig().getMetrics().getTagGuard();

        final String filename = tagGuardSettings.getDatabaseFile();
        if (StringUtils.isBlank(filename)) {
            log.error("TagGuard filename is empty. Not able to write attributes");
            return null;
        }

        return filename;
    }

    private void processNewAttributes(Map<String, Map<String, Set<String>>> storedAttributes) {
        Set<AttributesHolder> copy = latestAttributes;
        latestAttributes = Collections.synchronizedSet(new HashSet<>());

        // process new attributes
        copy.forEach(attributesHolder -> {
            String metricName = attributesHolder.getMetricName();
            Map<String, String> newAttributes = attributesHolder.getAttributes();
            int maxValuesPerAttribute = getMaxValuesPerAttribute(metricName, env.getCurrentConfig());

            Map<String, Set<String>> attributeValuesByKey = storedAttributes.computeIfAbsent(metricName, k -> Maps.newHashMap());
            newAttributes.forEach((attributeKey, attributeValue) -> {
                Set<String> attributeValues = attributeValuesByKey.computeIfAbsent(attributeKey, (x) -> new HashSet<>());
                // if attribute value is new AND max values per attribute is already reached
                if (!attributeValues.contains(attributeValue) && attributeValues.size() >= maxValuesPerAttribute) {
                    boolean isNewBlockedAttribute = blockedAttributeKeysByMetric.computeIfAbsent(metricName, metric -> Sets.newHashSet()).add(attributeKey);
                    if (isNewBlockedAttribute) {
                        agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(),
                                String.format(attributeOverFlowMessageTemplate, metricName, attributeKey));
                        hasValueOverflow = true;
                    }
                } else {
                    attributeValues.add(attributeValue);
                }
            });
        });
    }

    private void removeBlockedAttributes(Map<String, Map<String, Set<String>>> availableAttributesByMetric) {
        // remove all blocked attributes, if no values are stored in the database file
        if (availableAttributesByMetric.isEmpty()) blockedAttributeKeysByMetric.clear();

        // independent of processing new attributes, check if attributes should be blocked or unblocked due to their value limit
        availableAttributesByMetric.forEach((metricName, attributes) -> {
            int maxValuesPerAttribute = getMaxValuesPerAttribute(metricName, env.getCurrentConfig());
            attributes.forEach((attributeKey, attributeValues) -> {
                if (attributeValues.size() >= maxValuesPerAttribute) {
                    boolean isNewBlockedAttribute = blockedAttributeKeysByMetric.computeIfAbsent(metricName, metric -> Sets.newHashSet())
                            .add(attributeKey);
                    if (isNewBlockedAttribute) {
                        agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(),
                                String.format(attributeOverFlowMessageTemplate, metricName, attributeKey));
                        hasValueOverflow = true;
                    }
                } else {
                    blockedAttributeKeysByMetric.getOrDefault(metricName, Sets.newHashSet()).remove(attributeKey);
                }
            });
        });
    }

    @Value
    @EqualsAndHashCode
    private static class AttributesHolder {

        String metricName;

        Map<String, String> attributes;
    }
}
