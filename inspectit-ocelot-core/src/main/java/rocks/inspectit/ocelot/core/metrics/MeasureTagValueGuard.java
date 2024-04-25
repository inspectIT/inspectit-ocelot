package rocks.inspectit.ocelot.core.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
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
import rocks.inspectit.ocelot.core.metrics.tagGuards.PersistedTagsReaderWriter;
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;
import rocks.inspectit.ocelot.core.tags.TagUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MeasureTagValueGuard {
    private static final String tagOverFlowMessageTemplate = "Overflow in measure %s for tag key %s";
    /**
     * Map of measure names and their related set of tag keys, which are currently blocked.
     */
    private final Map<String, Set<String>> blockedTagKeysByMeasure = Maps.newHashMap();
    PersistedTagsReaderWriter fileReaderWriter;
    @Autowired
    private InspectitEnvironment env;
    @Autowired
    private AgentHealthManager agentHealthManager;
    /**
     * Common tags manager needed for gathering common tags when recording metrics.
     */
    @Autowired
    private CommonTagsManager commonTagsManager;
    @Autowired
    private ScheduledExecutorService executor;
    private volatile boolean isShuttingDown = false;
    private boolean hasTagValueOverflow = false;
    private Set<TagsHolder> latestTags = Collections.synchronizedSet(new HashSet<>());
    private Future<?> blockTagValuesFuture;

    @PostConstruct
    protected void init() {
        scheduleTagGuardJob();
    }

    private void scheduleTagGuardJob() {
        Duration tagGuardScheduleDelay = env.getCurrentConfig().getMetrics().getTagGuard().getScheduleDelay();
        blockTagValuesFuture = executor.schedule(blockTagValuesTask, tagGuardScheduleDelay.toNanos(), TimeUnit.NANOSECONDS);
    }


    @PreDestroy
    protected void stop() {
        if (isTagGuardDisabled()) {
            return;
        }

        isShuttingDown = true;
        blockTagValuesFuture.cancel(true);
    }

    /**
     * Gets the max value amount per tag for the given measure by hierarchically extracting
     * {@link MetricDefinitionSettings#maxValuesPerTag} (prio 1),
     * {@link TagGuardSettings#maxValuesPerTagByMeasure} (prio 2) and
     * {@link TagGuardSettings#maxValuesPerTag} (default).
     *
     * @param measureName the current measure
     * @return The maximum amount of tag values for the given measure
     */
    @VisibleForTesting
    int getMaxValuesPerTag(String measureName, InspectitConfig config) {
        int maxValuesPerTag = config.getMetrics().getDefinitions().get(measureName).getMaxValuesPerTag();

        if (maxValuesPerTag > 0) return maxValuesPerTag;

        Map<String, Integer> maxValuesPerTagPerMeasuresMap = config.getMetrics()
                .getTagGuard()
                .getMaxValuesPerTagByMeasure();
        return maxValuesPerTagPerMeasuresMap.getOrDefault(measureName, config.getMetrics()
                .getTagGuard()
                .getMaxValuesPerTag());
    }

    /**
     * Creates the full tag context, including all specified tags, for the current measure
     *
     * @param context        current context
     * @param metricAccessor accessor for the measure as well as the particular tags
     * @return TagContext including all tags for the current measure
     */
    public TagContext getTagContext(IHookAction.ExecutionContext context, MetricAccessor metricAccessor) {
        Map<String, String> tags = Maps.newHashMap();
        String measureName = metricAccessor.getName();
        InspectitContextImpl inspectitContext = context.getInspectitContext();
        TagGuardSettings tagGuardSettings = env.getCurrentConfig().getMetrics().getTagGuard();

        Set<String> blockedTagKeys = blockedTagKeysByMeasure.getOrDefault(measureName, Sets.newHashSet());
        log.debug("Currently blocked tag keys for measure {}, due to exceeding the configured tag value limit: {}",
                measureName, blockedTagKeys);

        // first common tags to allow to overwrite by constant or data tags
        commonTagsManager.getCommonTagKeys().forEach(commonTagKey -> {
            Optional.ofNullable(inspectitContext.getData(commonTagKey.getName()))
                    .ifPresent(value -> tags.put(commonTagKey.getName(), TagUtils.createTagValueAsString(commonTagKey.getName(), value.toString())));
        });

        // then constant tags to allow to overwrite by data
        metricAccessor.getConstantTags().forEach((key, value) -> {
            if (tagGuardSettings.isEnabled() && blockedTagKeys.contains(key)) {
                String overflowReplacement = env.getCurrentConfig().getMetrics().getTagGuard().getOverflowReplacement();
                tags.put(key, TagUtils.createTagValueAsString(key, overflowReplacement));
            } else {
                tags.put(key, TagUtils.createTagValueAsString(key, value));
            }
        });

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTagAccessors().forEach((key, accessor) -> {
            if (tagGuardSettings.isEnabled() && blockedTagKeys.contains(key)) {
                String overflowReplacement = env.getCurrentConfig().getMetrics().getTagGuard().getOverflowReplacement();
                tags.put(key, TagUtils.createTagValueAsString(key, overflowReplacement));
            } else {
                Optional.ofNullable(accessor.get(context))
                        .ifPresent(tagValue -> tags.put(key, TagUtils.createTagValueAsString(key, tagValue.toString())));
            }
        });

        TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
        tags.forEach((key, value) -> tagContextBuilder.putLocal(TagKey.create(key), TagUtils.createTagValue(key, value)));

        // store the new tags for this measure as simple object and delay traversing trough tagKeys to async job
        latestTags.add(new TagsHolder(measureName, tags));

        return tagContextBuilder.build();

    }

    private boolean isTagGuardDisabled() {
        return !env.getCurrentConfig().getMetrics().getTagGuard().isEnabled();
    }

    /**
     * Task, which reads the persisted tag values to determine, which tags should be blocked, because of exceeding
     * the specific tag value limit.
     * If new tags values have been created, they will be persisted.
     */
    @VisibleForTesting
    Runnable blockTagValuesTask = () -> {
        if (isNotWritable()) {
            return;
        }

        Map<String, Map<String, Set<String>>> storedTags = fileReaderWriter.read();
        processNewTags(storedTags);
        fileReaderWriter.write(storedTags);
        removeBlockedTags(storedTags);

        // invalidate incident, if tag overflow was detected, but no more tags are blocked
        boolean noBlockedTagKeys = blockedTagKeysByMeasure.values().stream().allMatch(Set::isEmpty);
        if (hasTagValueOverflow && noBlockedTagKeys) {
            agentHealthManager.invalidateIncident(this.getClass(), "Overflow for tags resolved");
            hasTagValueOverflow = false;
        }

        if (!isShuttingDown) scheduleTagGuardJob();
    };

    private boolean isNotWritable() {
        if (isTagGuardDisabled()) {
            return true;
        }

        initTagReaderWriter();
        return Objects.isNull(fileReaderWriter);
    }

    private void initTagReaderWriter() {
        final String filename = getFilename();
        if (Objects.nonNull(filename)) {
            fileReaderWriter = PersistedTagsReaderWriter.of(filename);
        }
    }

    private String getFilename() {
        TagGuardSettings tagGuardSettings = env.getCurrentConfig().getMetrics().getTagGuard();
        if (!tagGuardSettings.isEnabled()) {
            log.error("Filename is not set. Not able to be writing tags.");
            return null;
        }

        final String filename = tagGuardSettings.getDatabaseFile();
        if (StringUtils.isBlank(filename)) {
            log.error("Filename is empty. Not able to be writing tags.");
            return null;
        }

        log.info(String.format("TagValueGuard started with scheduleDelay %s and database file %s", tagGuardSettings.getScheduleDelay(), tagGuardSettings.getDatabaseFile()));
        return filename;
    }

    private void processNewTags(Map<String, Map<String, Set<String>>> storedTags) {
        Set<TagsHolder> copy = latestTags;
        latestTags = Collections.synchronizedSet(new HashSet<>());

        // process new tags
        copy.forEach(tagsHolder -> {
            String measureName = tagsHolder.getMeasureName();
            Map<String, String> newTags = tagsHolder.getTags();
            int maxValuesPerTag = getMaxValuesPerTag(measureName, env.getCurrentConfig());

            Map<String, Set<String>> tagValuesByTagKey = storedTags.computeIfAbsent(measureName, k -> Maps.newHashMap());
            newTags.forEach((tagKey, tagValue) -> {
                Set<String> tagValues = tagValuesByTagKey.computeIfAbsent(tagKey, (x) -> new HashSet<>());
                // if tag value is new AND max values per tag is already reached
                if (!tagValues.contains(tagValue) && tagValues.size() >= maxValuesPerTag) {
                    boolean isNewBlockedTag = blockedTagKeysByMeasure.computeIfAbsent(measureName, measure -> Sets.newHashSet()).add(tagKey);
                    if (isNewBlockedTag) {
                        agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(),
                                String.format(tagOverFlowMessageTemplate, measureName, tagKey));
                        hasTagValueOverflow = true;
                    }
                } else {
                    tagValues.add(tagValue);
                }
            });

        });
    }

    private void removeBlockedTags(Map<String, Map<String, Set<String>>> availableTagsByMeasure) {
        // remove all blocked tags, if no values are stored in the database file
        if (availableTagsByMeasure.isEmpty()) blockedTagKeysByMeasure.clear();

        // independent of processing new tags, check if tags should be blocked or unblocked due to their tag value limit
        availableTagsByMeasure.forEach((measureName, tags) -> {
            int maxValuesPerTag = getMaxValuesPerTag(measureName, env.getCurrentConfig());
            tags.forEach((tagKey, tagValues) -> {
                if (tagValues.size() >= maxValuesPerTag) {
                    boolean isNewBlockedTag = blockedTagKeysByMeasure.computeIfAbsent(measureName, measure -> Sets.newHashSet())
                            .add(tagKey);
                    if (isNewBlockedTag) {
                        agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(),
                                String.format(tagOverFlowMessageTemplate, measureName, tagKey));
                        hasTagValueOverflow = true;
                    }
                } else {
                    blockedTagKeysByMeasure.getOrDefault(measureName, Sets.newHashSet()).remove(tagKey);
                }
            });
        });
    }

    @Value
    @EqualsAndHashCode
    private static class TagsHolder {
        String measureName;
        Map<String, String> tags;
    }




}


