package rocks.inspectit.oce.core.tags;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
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
     * All common tags a simple String map.
     */
    @Getter
    private Map<String, String> commonTagValueMap;

    /**
     * OpenCensus tag context representing common tag context.
     */
    @Getter
    private TagContext commonTagContext = Tags.getTagger().emptyBuilder().build();

    /**
     * List of common tag keys that can be used when creating the views.
     */
    private List<TagKey> commonTagKeys = Collections.emptyList();

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
     * Registers a tag provider.
     *
     * @param tagsProvider ITagProvider
     */
    public void register(ITagsProvider tagsProvider) {
        if (!providers.contains(tagsProvider)) {
            providers.add(tagsProvider);
            createCommonTagContext();
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
            createCommonTagContext();
        }
    }

    /**
     * Utility to ensure that all ITagsProviders register themselves before the CommonTagsManager is used somewhere else.
     *
     * @param providers
     */
    @Autowired
    void setAllTagsProviders(List<ITagsProvider> providers) {
    }

    /**
     * Processes all {@link #providers} and creates common context based on the providers priority.
     */
    private void createCommonTagContext() {
        // first create map of tags based on the providers priority
        Map<String, String> newCommonTagValueMap = new HashMap<>();
        providers.stream()
                .sorted(Comparator.comparingInt(ITagsProvider::getPriority).reversed())
                .forEach(provider -> provider.getTags().forEach(newCommonTagValueMap::putIfAbsent));

        // then create key/value tags pairs for resolved map
        List<TagKey> newCommonTagKeys = new ArrayList<>();
        TagContextBuilder tagContextBuilder = Tags.getTagger().emptyBuilder();
        newCommonTagValueMap.forEach((k, v) -> {
            TagKey key = TagKey.create(k);
            newCommonTagKeys.add(key);
            tagContextBuilder.put(key, TagValue.create(v));
        });
        commonTagKeys = newCommonTagKeys;
        commonTagValueMap = newCommonTagValueMap;
        commonTagContext = tagContextBuilder.build();
    }

}
