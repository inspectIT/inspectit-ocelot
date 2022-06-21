package rocks.inspectit.ocelot.core.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
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
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
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

    @Autowired
    private InspectitEnvironment env;

    /**
     * Common tags manager needed for gathering common tags when recording metrics.
     */
    @Autowired
    private CommonTagsManager commonTagsManager;

    @Autowired
    private ScheduledExecutorService executorService;

    private PersistedTagsReaderWriter fileReaderWriter;

    private volatile boolean isShuttingDown = false;

    private Map<String, Map<String, Boolean>> blockedTagKeysByMeasure = Maps.newHashMap();

    private Set<TagsHolder> latestTags = Collections.synchronizedSet(new HashSet<>());

    private Future<?> blockTagValuesFuture;

    @PostConstruct
    protected void init() {
        fileReaderWriter = new PersistedTagsReaderWriter(env.getCurrentConfig()
                .getMetrics()
                .getTagGuardDatabaseFile(), new ObjectMapper());

        blockTagValuesTask.run();
        scheduleTagGuardJob();
    }

    private void scheduleTagGuardJob() {
        Duration tagGuardScheduleDelay = env.getCurrentConfig().getMetrics().getTagGuardScheduleDelay();
        blockTagValuesFuture = executorService.schedule(blockTagValuesTask, tagGuardScheduleDelay.toNanos(), TimeUnit.NANOSECONDS);
    }

    @PreDestroy
    protected void stop() {
        isShuttingDown = true;
        blockTagValuesFuture.cancel(true);
    }

    Runnable blockTagValuesTask = () -> {

        Set<TagsHolder> copy = latestTags;
        latestTags = Collections.synchronizedSet(new HashSet<>());

        Map<String, Map<String, Set<String>>> availableTagsByMeasure = fileReaderWriter.read();

        copy.forEach(t -> {
            String measureName = t.getMeasureName();
            Map<String, String> newTags = t.getTags();
            int tagValueLimit = env.getCurrentConfig()
                    .getMetrics()
                    .getDefinitions()
                    .get(measureName)
                    .getTagValueLimit();

            Map<String, Set<String>> tagValuesByTagKey = availableTagsByMeasure.computeIfAbsent(measureName, k -> Maps.newHashMap());
            newTags.forEach((tagKey, tagValue) -> {
                Set<String> tagValues = tagValuesByTagKey.computeIfAbsent(tagKey, (x) -> new HashSet<>());
                if (tagValues.size() > tagValueLimit) {
                    blockedTagKeysByMeasure.computeIfAbsent(tagKey, blocked -> Maps.newHashMap())
                            .putIfAbsent(tagKey, true);
                } else {
                    tagValues.add(tagValue);
                }
            });

        });

        fileReaderWriter.write(availableTagsByMeasure);

        if (!isShuttingDown) {
            scheduleTagGuardJob();
        }

    };

    public TagContext getTagContext(IHookAction.ExecutionContext context, MetricAccessor metricAccessor) {
        Map<String, String> tags = Maps.newHashMap();
        String measureName = metricAccessor.getName();
        InspectitContextImpl inspectitContext = context.getInspectitContext();
        Map<String, Boolean> blockedTageKeys = blockedTagKeysByMeasure.getOrDefault(measureName, Maps.newHashMap());

        // first common tags to allow to overwrite by constant or data tags
        commonTagsManager.getCommonTagKeys().forEach(commonTagKey -> {
            if (!blockedTageKeys.containsKey(commonTagKey.getName())) {
                Optional.ofNullable(inspectitContext.getData(commonTagKey.getName()))
                        .ifPresent(value -> tags.put(commonTagKey.getName(), TagUtils.createTagValueAsString(commonTagKey.getName(), value.toString())));
            } else {
                tags.put(commonTagKey.getName(), "Bist du ne ID?");
            }
        });

        // then constant tags to allow to overwrite by data
        metricAccessor.getConstantTags().forEach((key, value) -> {
            if (!blockedTageKeys.containsKey(key)) {
                tags.put(key, TagUtils.createTagValueAsString(key, value));
            } else {
                //blocked
            }
        });

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTagAccessors().forEach((key, accessor) -> {
            if (!blockedTageKeys.containsKey(key)) {
                Optional.ofNullable(accessor.get(context))
                        .ifPresent(tagValue -> tags.put(key, TagUtils.createTagValueAsString(key, tagValue.toString())));
            } else {
                //blocked
            }
        });

        TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
        tags.forEach((key, value) -> tagContextBuilder.putLocal(TagKey.create(key), TagUtils.createTagValue(key, value)));

        // Store the new tags for this measure as simple object and delay traversing trough tagKeys to async job
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
                        @SuppressWarnings("unchecked") Map<String, Map<String, Set<String>>> tags = mapper.readValue(content, Map.class);
                        return tags;
                    } catch (Exception e) {
                        log.error("Error loading tag value database from persistence file '{}'", fileName, e);
                    }
                } else {
                    log.info("No tag value database available. Assuming first Agent deployment.");
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
                    log.error("Error writing tag value database to file '{}'", fileName, e);
                }
            }
        }
    }

}
