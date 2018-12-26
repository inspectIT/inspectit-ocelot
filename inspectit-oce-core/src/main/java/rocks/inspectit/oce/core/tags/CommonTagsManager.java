package rocks.inspectit.oce.core.tags;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Component that provides tags that should be considered as common and used when ever a metric is recorded.
 */
@Component
public class CommonTagsManager {

    /**
     * All {@link ITagsProvider}s registered in the manager.
     */
    private final List<ITagsProvider> providers = new CopyOnWriteArrayList<>();

    /**
     * OpenCensus tag context representing common tag context.
     */
    private TagContext commonTagContext;

    /**
     * List of common tag keys that can be used when creating the views.
     */
    private List<TagKey> commonTagKeys;

    /**
     * Returns common tags keys that all view should register.
     *
     * @return Returns common tags keys that all view should register.
     */
    public List<TagKey> getCommonTagKeys() {
        return Collections.unmodifiableList(commonTagKeys);
    }

    /**
     * Returns newly created scope with common tag context. Metrics collectors should use this Scope with the try/resource block:
     * <code>
     * try (Scope scope = withCommonTagScope()) {
     * Stats.getStatsRecorder().newMeasureMap().put(M_ERRORS, 1L).record();
     * }
     * </code>
     *
     * @return Returns newly created scope with default tag context.
     */
    public Scope withCommonTagScope() {
        return Tags.getTagger().withTagContext(commonTagContext);
    }

    /**
     * Returns default tag context. For testing.
     *
     * @return Returns default tag context.
     */
    TagContext getCommonTagContext() {
        return commonTagContext;
    }

    /**
     * Registers a tag provider.
     *
     * @param tagsProvider ITagProvider
     */
    public void register(ITagsProvider tagsProvider) {
        if (!providers.contains(tagsProvider)) {
            providers.add(tagsProvider);
            createDefaults();
        }
    }

    /**
     * Unregisters a tag provider.
     *
     * @param tagsProvider ITagProvider
     */
    public void unregister(ITagsProvider tagsProvider) {
        if (providers.contains(tagsProvider)) {
            providers.remove(tagsProvider);
            createDefaults();
        }
    }

    /**
     * Processes all {@link #providers} on the start-up.
     */
    protected void createDefaults() {
        // first create map of tags based on the providers priority
        Map<String, String> all = new HashMap<>();
        providers.stream()
                .sorted(Comparator.comparingInt(ITagsProvider::getPriority).reversed())
                .forEach(provider -> provider.getTags().forEach(all::putIfAbsent));

        // then create key/value tags pairs for resolved map
        commonTagKeys = new ArrayList<>();
        TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
        all.forEach((k, v) -> {
            TagKey key = TagKey.create(k);
            commonTagKeys.add(key);
            tagContextBuilder.put(key, TagValue.create(v));
        });
        commonTagContext = tagContextBuilder.build();
    }

}
