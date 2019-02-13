package rocks.inspectit.oce.core.instrumentation.context;

import io.grpc.Context;
import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.*;
import java.util.stream.Stream;


public class InspectitContext implements AutoCloseable {

    /**
     * We only allow "data" of the following types to be used as tags
     */
    private static final Set<Class<?>> ALLOWED_TAG_TYPES = new HashSet<>(Arrays.asList(
            String.class, Character.class, Long.class, Integer.class, Short.class, Byte.class,
            Double.class, Float.class, Boolean.class
    ));

    static final Context.Key<InspectitContext> INSPECTIT_KEY = Context.key("inspectit-immutableData");

    private InspectitContext parent;

    private Context overridenGrpcContext;

    private TagContext openedDownPropagationContext = null;
    private Scope openedDownPropagationScope = null;

    //data collect in the entry face- this will be visible when the context is active
    //this only contains down-propagated data! everything else is stored only in dataOverwrites
    private Map<String, Object> downPropagatedData;

    //Contains all data which was overwritten by this context in compariso nto the data stored in parent
    //this means that this map contains all the data fro mthe entry phase, exit phas and everything that was up-propagated from children
    //this map explicitly allows null value, representing that
    private final Map<String, Object> dataOverwrites;

    private final ResolvedDataProperties propagation;

    private final Thread openingThread;

    InspectitContext(InspectitContext parent, ResolvedDataProperties propagation) {
        this.parent = parent;
        this.propagation = propagation;


        downPropagatedData = parent == null ? Collections.emptyMap() : parent.downPropagatedData;
        dataOverwrites = new HashMap<>();

        openingThread = Thread.currentThread();
    }

    public static InspectitContext createFromCurrent(CommonTagsManager commonTagsManager, ResolvedDataProperties propagation, boolean inheritValuesFromCurrentTagContext) {
        InspectitContext parent = INSPECTIT_KEY.get();
        InspectitContext result = new InspectitContext(parent, propagation);

        if (parent == null) {
            commonTagsManager.getCommonTagValueMap()
                    .forEach(result::setData);
        }

        if (inheritValuesFromCurrentTagContext) {
            TagContext currentTags = Tags.getTagger().getCurrentTagContext();
            if (parent == null) {
                for (Iterator<Tag> it = InternalUtils.getTags(currentTags); it.hasNext(); ) {
                    Tag tag = it.next();
                    result.setData(tag.getKey().getName(), tag.getValue().asString());
                }
            } else if (currentTags != parent.openedDownPropagationContext) {
                for (Iterator<Tag> it = InternalUtils.getTags(currentTags); it.hasNext(); ) {
                    Tag tag = it.next();
                    String tagKey = tag.getKey().getName();
                    String tagValue = tag.getValue().asString();
                    Object parentValueForTag = parent.downPropagatedData.get(tagKey);
                    if (parentValueForTag == null || !parentValueForTag.toString().equals(tagValue)) {
                        result.setData(tagKey, tagValue);
                    }
                }
            }
        }

        return result;
    }

    public void makeCurrent(boolean openTagContext) {
        boolean anyDownPropagatedDataOverwritten = dataOverwrites.keySet().stream()
                .anyMatch(propagation::isPropagatedDownWithinJVM);

        if (anyDownPropagatedDataOverwritten) {
            downPropagatedData = new HashMap<>(downPropagatedData);
            dataOverwrites.entrySet().stream()
                    .filter(e -> propagation.isPropagatedDownWithinJVM(e.getKey()))
                    .forEach(e -> downPropagatedData.put(e.getKey(), e.getValue()));
        }

        overridenGrpcContext = Context.current().withValue(INSPECTIT_KEY, this).attach();

        if (openTagContext) {
            Tagger tagger = Tags.getTagger();
            openedDownPropagationScope = tagger.withTagContext(new TagContext() {
                @Override
                protected Iterator<Tag> getIterator() {
                    return getTagIterator();
                }
            });
            //unfortunately the open-census implementation performs a copy on the call above
            //resulting in the current context not being the same object as "this"
            openedDownPropagationContext = tagger.getCurrentTagContext();
        }

    }

    public Scope enterTagScopeWithOverrides() {
        TagContextBuilder builder = Tags.getTagger().emptyBuilder();
        Stream.concat( //combine the down propagated data with the overwrites
                downPropagatedData.entrySet().stream()
                        .filter(e -> !dataOverwrites.containsKey(e.getKey())),
                dataOverwrites.entrySet().stream()
        )
                .filter(e -> propagation.isTag(e.getKey()))
                .filter(e -> e.getValue() != null)
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()))
                .forEach(e -> builder.put(TagKey.create(e.getKey()), TagValue.create(e.getValue().toString())));
        return builder.buildScoped();
    }

    public Object getData(String key) {
        if (dataOverwrites.containsKey(key)) {
            return dataOverwrites.get(key);
        } else {
            return downPropagatedData.get(key);
        }
    }

    public void setData(String key, Object value) {
        dataOverwrites.put(key, value);
    }


    @Override
    public void close() {
        if (openedDownPropagationScope != null) {
            openedDownPropagationScope.close();
        }
        Context.current().detach(overridenGrpcContext);

        if (parent != null && Thread.currentThread() == parent.openingThread) {
            parent.performUpPropagation(dataOverwrites);
        }
        //clear the references to prevent memory leaks
        parent = null;
        overridenGrpcContext = null;
    }

    private void performUpPropagation(Map<String, Object> childData) {
        childData.entrySet().stream()
                .filter(e -> propagation.isPropagatedUpWithinJVM(e.getKey()))
                .forEach(e -> dataOverwrites.put(e.getKey(), e.getValue()));
    }

    protected Iterator<Tag> getTagIterator() {
        return downPropagatedData.entrySet().stream()
                .filter(e -> propagation.isTag(e.getKey()))
                .filter(e -> e.getValue() != null)
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()))
                .map(e -> Tag.create(TagKey.create(e.getKey()), TagValue.create(e.getValue().toString())))
                .iterator();
    }
}
