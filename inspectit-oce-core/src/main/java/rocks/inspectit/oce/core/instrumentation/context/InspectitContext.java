package rocks.inspectit.oce.core.instrumentation.context;

import io.grpc.Context;
import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.*;

public class InspectitContext implements AutoCloseable {

    /**
     * Package private for testing
     */
    static final Context.Key<InspectitContext> INSPECTIT_KEY = Context.key("inspectit-data");

    /**
     * We only allow "data" of the following types to be used as tags
     */
    private static final Set<Class<?>> ALLOWED_TAG_TYPES = new HashSet<>(Arrays.asList(
            String.class, Character.class, Long.class, Integer.class, Short.class, Byte.class,
            Double.class, Float.class, Boolean.class
    ));

    private Map<String, Object> data = new HashMap<>();
    private Set<String> writtenUpPropagatingKeys = null;

    private Context parentGRPCContext;
    private InspectitContext parent;

    private Scope openedDownPropagationTagScope;
    private TagContext openedDownPropagationTagContext;

    private ResolvedDataProperties dataProperties;

    private InspectitContext() {
    }

    public static InspectitContext createAndEnter(CommonTagsManager commonTags, ResolvedDataProperties dataProperties) {
        InspectitContext parent = INSPECTIT_KEY.get();
        TagContext parentTags = Tags.getTagger().getCurrentTagContext();

        InspectitContext result = new InspectitContext();

        result.dataProperties = dataProperties;
        result.parent = parent;
        if (parent == null) {
            result.data.putAll(commonTags.getCommonTagValueMap());
        } else {
            parent.data.entrySet().stream()
                    .filter(e -> dataProperties.isPropagatedDownWithinJVM(e.getKey()))
                    .forEach(e -> result.data.put(e.getKey(), e.getValue()));
        }

        if (parentTags != null) {
            //we copy the data from any opened tag context in case it was not opened by the inspectit parent context
            //if the TagContext comes from the parent InpsectitContext, the data has already been copied
            if (parent == null || (parent.openedDownPropagationTagContext != parentTags)) {
                //only read back values which have benn overwritten
                Map<String, String> publishedParentTags = new HashMap<>();
                if (parent != null && parent.openedDownPropagationTagContext != null) {
                    InternalUtils.getTags(parent.openedDownPropagationTagContext).forEachRemaining(tag -> {
                        publishedParentTags.put(tag.getKey().getName(), tag.getValue().asString());
                    });
                }

                InternalUtils.getTags(parentTags).forEachRemaining(tag -> {
                    String key = tag.getKey().getName();
                    String value = tag.getValue().asString();
                    //why do we do this check? When publishing data as tags, the values get converted to Strings.
                    //this means that e.g. a long value 42L get turned to "42"
                    // if we now just read back this value without checking if it actually changed,
                    // we would convert the "Long" value to a "String" which we do not want to happen
                    if (!Objects.equals(value, publishedParentTags.get(key))) {
                        result.data.put(key, value);
                    }
                });
            }
        }
        result.parentGRPCContext = Context.current().withValue(INSPECTIT_KEY, result).attach();
        return result;
    }

    public void setData(String key, Object value) {
        data.put(key, value);
        if (dataProperties.isPropagatedUpWithinJVM(key)) {
            if (writtenUpPropagatingKeys == null) {
                writtenUpPropagatingKeys = new HashSet<>();
            }
            writtenUpPropagatingKeys.add(key);
        }
    }

    public Object getData(String key) {
        return data.get(key);
    }

    @Override
    public void close() {
        Context.current().detach(parentGRPCContext);
        if (parent != null && writtenUpPropagatingKeys != null) {
            parent.performUpPropagation(data, writtenUpPropagatingKeys);
        }
        // Event though this context is closed it might be referenced by async calls which where triggered
        // for this reason we need to clear the parent to prevent memory leaks
        // comment these lines out and run the unit tests to see what the issue is
        parent = null;
        parentGRPCContext = null;
    }

    public void enterTagContextWithOnlyDownPropagatedData() {
        Tagger tagger = Tags.getTagger();
        TagContextBuilder result = tagger.emptyBuilder();
        data.entrySet().stream()
                .filter(e -> dataProperties.isTag(e.getKey()))
                .filter(e -> dataProperties.isPropagatedDownWithinJVM(e.getKey()))
                .filter(e -> e.getValue() != null)
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()))
                .forEach(e -> result.put(TagKey.create(e.getKey()), TagValue.create(e.getValue().toString())));

        openedDownPropagationTagContext = result.build();
        openedDownPropagationTagScope = tagger.withTagContext(openedDownPropagationTagContext);
    }


    public Scope enterTagScopeWithAllData() {
        Tagger tagger = Tags.getTagger();
        TagContextBuilder result = tagger.emptyBuilder();
        data.entrySet().stream()
                .filter(e -> dataProperties.isTag(e.getKey()))
                .filter(e -> e.getValue() != null)
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()))
                .forEach(e -> result.put(TagKey.create(e.getKey()), TagValue.create(e.getValue().toString())));

        return result.buildScoped();
    }


    public void exitTagContextWithOnlyDownPropagation() {
        openedDownPropagationTagScope.close();
        openedDownPropagationTagContext = null;
    }

    /**
     * @param childData
     * @param dataKeysToPropagate
     */
    private void performUpPropagation(Map<String, Object> childData, Set<String> dataKeysToPropagate) {
        dataKeysToPropagate.forEach(k -> data.put(k, childData.get(k)));
        if (writtenUpPropagatingKeys == null) {
            writtenUpPropagatingKeys = new HashSet<>(dataKeysToPropagate);
        } else {
            writtenUpPropagatingKeys.addAll(dataKeysToPropagate);
        }

        if (openedDownPropagationTagContext != null) {
            //Upropagation within opencensus tagContext only works correctly if no other tag context was opened in between
            //if this is not the case we have to accept the fact that uppropagation changes will not be visible in the tags of application metrics
            //until the openedDownPropagationTagContext is closed
            if (Tags.getTagger().getCurrentTagContext() == openedDownPropagationTagContext) {
                exitTagContextWithOnlyDownPropagation();
                enterTagContextWithOnlyDownPropagatedData();
            }
        }
    }

}
