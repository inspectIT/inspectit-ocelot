package rocks.inspectit.ocelot.core.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
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
import rocks.inspectit.ocelot.core.selfmonitoring.AgentHealthManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;
import rocks.inspectit.ocelot.core.tags.TagUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MeasureTagValueGuard {
    private static final String tagOverFlowMessageTemplate = "Overflow for tag %s";

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

    private PersistedTagsReaderWriter fileReaderWriter;

    private volatile boolean isShuttingDown = false;

    private boolean hasTagValueOverflow = false;

    /**
     * Map of measure names and their related set of tag keys, which are currently blocked.
     */
    private final Map<String, Set<String>> blockedTagKeysByMeasure = Maps.newHashMap();

    private Set<TagsHolder> latestTags = Collections.synchronizedSet(new HashSet<>());

    private Future<?> blockTagValuesFuture;

    @PostConstruct
    protected void init() {
        TagGuardSettings tagGuardSettings = env.getCurrentConfig().getMetrics().getTagGuard();
        if (!tagGuardSettings.isEnabled()) return;

        fileReaderWriter = new PersistedTagsReaderWriter(tagGuardSettings.getDatabaseFile(), new ObjectMapper());
        scheduleTagGuardJob();

        log.info(String.format("TagValueGuard started with scheduleDelay %s and database file %s", tagGuardSettings.getScheduleDelay(), tagGuardSettings.getDatabaseFile()));
    }

    private void scheduleTagGuardJob() {
        Duration tagGuardScheduleDelay = env.getCurrentConfig().getMetrics().getTagGuard().getScheduleDelay();
        blockTagValuesFuture = executor.schedule(blockTagValuesTask, tagGuardScheduleDelay.toNanos(), TimeUnit.NANOSECONDS);
    }

    @PreDestroy
    protected void stop() {
        if (!env.getCurrentConfig().getMetrics().getTagGuard().isEnabled()) return;

        isShuttingDown = true;
        blockTagValuesFuture.cancel(true);
    }

    /**
     * Task, which reads the persisted tag values to determine, which tags should be blocked, because of exceeding
     * the specific tag value limit.
     * If new tags values have been created, they will be persisted.
     */
    Runnable blockTagValuesTask = () -> {
        if (!env.getCurrentConfig().getMetrics().getTagGuard().isEnabled()) return;

        // read current tag value database
        Map<String, Map<String, Set<String>>> availableTagsByMeasure = fileReaderWriter.read();

        Set<TagsHolder> copy = latestTags;
        latestTags = Collections.synchronizedSet(new HashSet<>());

        // process new tags
        copy.forEach(tagsHolder -> {
            String measureName = tagsHolder.getMeasureName();
            Map<String, String> newTags = tagsHolder.getTags();
            int maxValuesPerTag = getMaxValuesPerTag(measureName, env.getCurrentConfig());

            Map<String, Set<String>> tagValuesByTagKey = availableTagsByMeasure.computeIfAbsent(measureName, k -> Maps.newHashMap());
            newTags.forEach((tagKey, tagValue) -> {
                Set<String> tagValues = tagValuesByTagKey.computeIfAbsent(tagKey, (x) -> new HashSet<>());
                // if tag value is new AND max values per tag is already reached
                if (!tagValues.contains(tagValue) && tagValues.size() >= maxValuesPerTag) {
                    blockedTagKeysByMeasure.computeIfAbsent(measureName, measure ->  Sets.newHashSet()).add(tagKey);
                    agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(), String.format(tagOverFlowMessageTemplate, tagKey));
                    hasTagValueOverflow = true;
                } else {
                    tagValues.add(tagValue);
                }
            });

        });

        fileReaderWriter.write(availableTagsByMeasure);

        // remove all blocked tags, if no values are stored in the database file
        if(availableTagsByMeasure.isEmpty()) blockedTagKeysByMeasure.clear();

        // independent of processing new tags, check if tags should be blocked or unblocked due to their tag value limit
        availableTagsByMeasure.forEach((measureName, tags) -> {
            int maxValuesPerTag = getMaxValuesPerTag(measureName, env.getCurrentConfig());
            tags.forEach((tagKey, tagValues) ->  {
                if(tagValues.size() >= maxValuesPerTag) {
                    boolean isNewBlockedTag = blockedTagKeysByMeasure.computeIfAbsent(measureName, measure -> Sets.newHashSet())
                            .add(tagKey);
                    if(isNewBlockedTag) {
                        agentHealthManager.handleInvalidatableHealth(AgentHealth.ERROR, this.getClass(), String.format(tagOverFlowMessageTemplate, tagKey));
                        hasTagValueOverflow = true;
                    }
                } else {
                    blockedTagKeysByMeasure.getOrDefault(measureName, Sets.newHashSet()).remove(tagKey);
                }
            });
        });

        // invalidate incident, if tag overflow was detected, but no more tags are blocked
        boolean noBlockedTagKeys = blockedTagKeysByMeasure.values().stream().allMatch(Set::isEmpty);
        if(hasTagValueOverflow && noBlockedTagKeys) {
            agentHealthManager.invalidateIncident(this.getClass(), "Overflow for tags resolved");
            hasTagValueOverflow = false;
        }

        if (!isShuttingDown) scheduleTagGuardJob();
    };

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
     * @param context current context
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

    @Value
    @EqualsAndHashCode
    private static class TagsHolder {

        String measureName;

        Map<String, String> tags;

    }

    @AllArgsConstructor
    static class PersistedTagsReaderWriter {

        @NonNull
        private String fileName;

        @NonNull
        private ObjectMapper mapper;

        public Map<String, Map<String, Set<String>>> read() {
            if (!StringUtils.isBlank(fileName)) {
                Path path = Paths.get(fileName);
                if (Files.exists(path)) {
                    try {
                        byte[] content = Files.readAllBytes(path);
                        @SuppressWarnings("unchecked") Map<String, Map<String, Set<String>>> tags = mapper.readValue(content, new TypeReference<Map<String, Map<String, Set<String>>>>() {
                        });
                        return tags;
                    } catch (Exception e) {
                        log.error("Error loading tag-guard database from persistence file '{}'", fileName, e);
                    }
                } else {
                    log.info("Could not find tag-guard database file. File will be created during next write");
                }
            }
            return Maps.newHashMap();
        }

        public void write(Map<String, Map<String, Set<String>>> tagValues) {
            if (!StringUtils.isBlank(fileName)) {
                try {
                    Path path = Paths.get(fileName);
                    Files.createDirectories(path.getParent());
                    String tagValuesString = mapper.writeValueAsString(tagValues);
                    Files.write(path, tagValuesString.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    log.error("Error writing tag-guard database to file '{}'", fileName, e);
                }
            }
        }
    }

}
