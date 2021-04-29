package rocks.inspectit.ocelot.core.instrumentation.context;

import io.grpc.Context;
import io.opencensus.common.Function;
import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.tags.TagUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * This class allows the storage and configurable up and down propagation of data.
 * An inspectIT context has four lifecycle phases which correspond to the phased of hooks added to methods:
 * <p>
 * Executed Method:   | <- entry hook -> | <- method body  -> | <- exit hook -> | (method has returned)
 * Context Lifecycle: | <- Entry Phase-> | <- Active Phase -> | <- Exit Phase-> |  (Closed)
 * <p>
 * When a method is entered, the entry hook is executed.
 * In this phase a new InspectitContextImpl is created but not yet made active.
 * This context inherits all down-propagated data from the currently active context as well as all tags from the active TagContext.
 * In the entry phase, the contexts data can be altered via {@link #setData(String, Object)}, even though the context is not yet active.
 * <p>
 * When {@link #makeActive()} is called, the current context transitions from the "Entry" to the "Active" state.
 * This means that the data the context stores is now immutable and also published as TagContext.
 * In addition, the context now replaces its parent in GRPC, so that all newly created contexts will be children of this one.
 * There is one exception to the data immutability: child contexts perform the data up-propagation during this contexts active phase.
 * <p>
 * All synchronous child contexts are opened and closed during the "active" phase of their parent.
 * When such a child context is closed, it writes the up-propagated data it changed to the parent by calling {@link #performUpPropagation(Map)}.
 * Note that this only happens if the child context is synchronous, no up-propagation is performed for asynchronous children!
 * <p>
 * The up-propagation does not have an effect on the tag-context opened by the parent during the "active" phase.
 * E.g. if a child adds or overwrites a value its parent through up-propagation, this new value will not be visible in the tag context.
 * However, as soon as a new, synchronous child context of the parent is created and it opens a tag context, the changes wil lbe visible in the new context
 * <p>
 * When data is configured to be both up and down propagated, the down propagated data depends on whether the child is synchronous or not.
 * Consider a simple example where the context "parent" has two children "firstChild" and "secondChild" which are executed after each other.
 * This means that "firstChild" performs its up-propagation before "secondChild" is created and enters the "Entry" phase.
 * If "secondChild" is synchronous, it will now inherit the changed data which was up-propagated by "firstChild".
 * If "secondChild" however is asynchronous, it will only see the data of "parent" how it was at the end of the "Entry" phase.
 * In this case, the up-propagation done by "firstChild" will be invisible for "secondChild".
 * This behaviour was chosen to prevent potential race conditions for asynchronous contexts.
 *
 * <p>
 * Finally the context will enter the "Exit" phase after its method has terminated.
 * At this point, all synchronous child contexts have been created and closed, meaning that their values have been up-propagated.
 * During the exit phase, the contexts data can be modified again.
 * Note again that for any asynchronous child at any point of time the parent data will be visible as it was after the parents entry phase.
 * <p>
 * As noted previously the tag context opened by the context at the end of its entry phase will be stale for teh exit phase:
 * It does not contain any up-propagated data, neither does it contain any changes performed during the exit phase.
 * For this reason, an up-to-date tag context can be acquired using {@link #enterFullTagScope()} within which then metrics can be collected.
 * <p>
 * Finally a context finishes the exit phase with a call to {@link #close()}
 * If the context is synchronous, it will perform its up-propagation.
 * In addition ,the tag-context opened by the call to makeActive will be closed and the
 * previous parent will be registered back in GRPC as active context.
 * <p>
 * In addition, an {@link InspectitContextImpl} instance can be used for tracing. Hereby, one instance can record exactly one span.
 * To do this {@link #enterSpan(Span)} must be called BEFORE {@link #makeActive()}.
 * The span is automatically finished when {@link #close()} is called.
 */
@Slf4j
public class InspectitContextImpl implements InternalInspectitContext {

    /**
     * We only allow "data" of the following types to be used as tags
     */
    private static final Set<Class<?>> ALLOWED_TAG_TYPES = new HashSet<>(Arrays.asList(String.class, Character.class, Long.class, Integer.class, Short.class, Byte.class, Double.class, Float.class, Boolean.class));

    static final Context.Key<InspectitContextImpl> INSPECTIT_KEY = Context.key("inspectit-context");

    /**
     * Points to the parent from which this context inherits its data and to which potential up-propagation is performed.
     * Is effectively final and never changes, except that it is set to null in {@link #close()} to prevent memory leaks.
     */
    private InspectitContextImpl parent;

    /**
     * Defines for each data key its propagation behaviour as well as if it is a tag.
     */
    private PropagationMetaData propagation;

    /**
     * Defines whether the context should interact with TagContexts opened by the instrumented application.
     * <p>
     * If this is true, the context will inherit all values from the current {@link TagContext} which was opened by the target application.
     * In addition if this value is true makeActive will open a TagContext containing all down propagated tags stored in this InspectIT context.
     */
    private final boolean interactWithApplicationTagContexts;

    /**
     * Contains the thread in which this context was created.
     * This is used to identify async traces by comparing their thread against the thread of their parent.
     */
    private final Thread openingThread;

    /**
     * Holds the previous GRPC context which was overridden when attaching this context as active in GRPC.
     */
    private Context overriddenGrpcContext;

    /**
     * The span which was (potentially) opened by invoking {@link #enterSpan(Span)}
     */
    private AutoCloseable currentSpanScope;

    /**
     * Holds the tag context which was opened by this context with the call to {@link #makeActive()}.
     * If none was opened, this variable is null.
     * Note that this tag context is not necessarily owned by this {@link InspectitContextImpl}.
     * If it did not change any value, the context can simply keep the current context and reference it using this variable.
     * <p>
     * The tag context is guaranteed to contain the same tags as returned by {@link #getPostEntryPhaseTags()}
     */
    private TagContext activePhaseDownPropagationTagContext;

    /**
     * Marker variable to indicate that {@link #activePhaseDownPropagationTagContext} is stale.
     * The tag context can become stale due to up-propagation when a child context up-propagates a new value for a tag which is present in the context.
     * This variable is used to indicate for child contexts that they should not reuse activePhaseDownPropagationTagContext
     * but instead should open a new tag context.
     */
    private boolean isActivePhaseDownPropagationTagContextStale;

    /**
     * If this context opened a new {@link #activePhaseDownPropagationTagContext} during {@link #makeActive()},
     * the corresponding scope is stored in this variable and will be used in {@link #close()} to clean up the context.
     */
    private Scope openedDownPropagationScope;

    /**
     * When a new context is created, this map contains the down-propagated data it inherited from its parent context.
     * During the entry phase, data updates are written to {@link #dataOverwrites}
     * When the entry phase terminates with a call to {@link #makeActive()}, this map is replaced with a new
     * one containing also the down-propagated data which has been newly written during the entry phase.
     * <p>
     * The underlying map must not change after the entry phase has terminated!
     * Asynchronous child context will use this map as source for down-propagated data!
     * <p>
     * Also, this map will never contain null values.
     * When a data key is assigned the value "null", the key will simply be not present in this map.
     */
    private Map<String, Object> postEntryPhaseDownPropagatedData;

    /**
     * Contains all writes performed via {@link #setData(String, Object)} during any life-cycle phase of the context.
     * This means that this map represents all data which has been altered during the lifetime of this context.
     * This also includes any writes performed due to the up-propagation of children.
     * <p>
     * The combination of {@link #postEntryPhaseDownPropagatedData} overwritten by this map therefore presents all current data.
     * <p>
     * Note that this map may contain null values: a null value indicates that the corresponding value has been cleared.
     * This is required for example to ensure clearing data is propagated up correctly.
     */
    private final Map<String, Object> dataOverwrites;

    /**
     * When a synchronous child context is opened during the active phase of its parent,
     * it inherits all {@link #postEntryPhaseDownPropagatedData} in combination with all down-propagated data from {@link #dataOverwrites}
     * With a naive implementation this result map would be recomputed for every child context, even if nothing has changed.
     * <p>
     * This map only changes when an-up propagation of data occurs which also is down propagated.
     * <p>
     * At the end of the entry phase, the map is the same as {@link #postEntryPhaseDownPropagatedData}
     * When now an up-propagation occurs, this map becomes stale. Therefore it is "reset" to null and recomputed when it is required.
     * <p>
     * Note that the underlying map never gets altered! It gets replaced by a new object when it became stale.
     * This ensures that child context can use this map as their {@link #postEntryPhaseDownPropagatedData} without copying!
     */
    private Map<String, Object> cachedActivePhaseDownPropagatedData = null;

    private InspectitContextImpl(InspectitContextImpl parent, PropagationMetaData defaultPropagation, boolean interactWithApplicationTagContexts) {
        this.parent = parent;
        propagation = parent == null ? defaultPropagation : parent.propagation;
        this.interactWithApplicationTagContexts = interactWithApplicationTagContexts;
        dataOverwrites = new HashMap<>();
        openingThread = Thread.currentThread();

        if (parent == null) {
            postEntryPhaseDownPropagatedData = Collections.emptyMap();
        } else {
            if (isInDifferentThreadThanParentOrIsParentClosed()) {
                postEntryPhaseDownPropagatedData = parent.postEntryPhaseDownPropagatedData;
            } else {
                //no copying required as the returned object is guaranteed to be immutable
                postEntryPhaseDownPropagatedData = parent.getOrComputeActivePhaseDownPropagatedData();
            }
        }
    }

    /**
     * Creates a new context which enters its "entry" lifecycle phase.
     * The created context will be a synchronous or asynchronous child of the currently active context.
     *
     * @param commonTags                         the common tags used to populate the data if this is a root context
     * @param defaultPropagation                 the data propagation settings to use if this is a root context. Otherwise the parent context's settings will be inherited.
     * @param interactWithApplicationTagContexts if true, data from the currently active {@link TagContext} will be inherited and makeActive will publish the data as a TagContext
     *
     * @return the newly created context
     */
    public static InspectitContextImpl createFromCurrent(Map<String, String> commonTags, PropagationMetaData defaultPropagation, boolean interactWithApplicationTagContexts) {
        InspectitContextImpl parent = INSPECTIT_KEY.get();
        InspectitContextImpl result = new InspectitContextImpl(parent, defaultPropagation, interactWithApplicationTagContexts);

        if (parent == null) {
            commonTags.forEach(result::setData);
        }

        if (interactWithApplicationTagContexts) {
            result.readOverridesFromCurrentTagContext();
        }

        return result;
    }

    public void setSpanScope(AutoCloseable spanScope) {
        currentSpanScope = spanScope;
    }

    /**
     * @return true, if {@link #enterSpan(Span, Function)} was called
     */
    public boolean hasEnteredSpan() {
        return currentSpanScope != null;
    }

    /**
     * Checks if previously a down propagation happened where a remote parent span was received.
     * If this is the case, the corresponding SpanContext is returned and removed from the context.
     *
     * @return the remote parent SpanContext received via down-propagation, null if none was received.
     */
    public SpanContext getAndClearCurrentRemoteSpanContext() {
        Object parent = getData(REMOTE_PARENT_SPAN_CONTEXT_KEY);
        if (parent instanceof SpanContext) {
            setData(REMOTE_PARENT_SPAN_CONTEXT_KEY, null);
            return (SpanContext) parent;
        } else {
            return null;
        }
    }

    /**
     * Terminates this contexts entry-phase and makes it the currently active context.
     */
    @Override
    public void makeActive() {
        boolean anyDownPropagatedDataOverwritten = anyDownPropagatedDataOverridden();

        //only copy if any down-propagating value has been written
        if (anyDownPropagatedDataOverwritten) {
            postEntryPhaseDownPropagatedData = getDownPropagatedDataAsNewMap();
        }
        cachedActivePhaseDownPropagatedData = postEntryPhaseDownPropagatedData;

        overriddenGrpcContext = Context.current().withValue(INSPECTIT_KEY, this).attach();

        if (interactWithApplicationTagContexts) {
            Tagger tagger = Tags.getTagger();
            //check if we can reuse the parent context
            if (anyDownPropagatedDataOverwritten || (parent != null && parent.isActivePhaseDownPropagationTagContextStale)) {
                openedDownPropagationScope = tagger.withTagContext(new TagContext() {
                    @Override
                    protected Iterator<Tag> getIterator() {
                        return getPostEntryPhaseTags();
                    }
                });
            }
            activePhaseDownPropagationTagContext = tagger.getCurrentTagContext();
        }
    }

    private boolean anyDownPropagatedDataOverridden() {
        for (String key : dataOverwrites.keySet()) {
            if (propagation.isPropagatedDownWithinJVM(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a view on the data of this context.
     * Note that this view is not thread safe, as it is affected by setData calls and up-propagation.
     *
     * @return a view on all the data available in this context.
     */
    @Override
    public Iterable<Map.Entry<String, Object>> getData() {
        return () -> getDataAsStream().iterator();
    }

    /**
     * @return true, if {@link #makeActive()} was called but {@link #close()} was not called yet
     */
    public boolean isInActiveOrExitPhase() {
        return overriddenGrpcContext != null;
    }

    /**
     * @return true, if this context should perform NO up propagation and only
     * inherit the {@link #postEntryPhaseDownPropagatedData} from its parent.
     */
    private boolean isInDifferentThreadThanParentOrIsParentClosed() {
        return parent != null && (parent.openingThread != openingThread || !parent.isInActiveOrExitPhase());
    }

    /**
     * Enters a new tag scope, which contains the tags currently present in {@link #getData()}.
     * In contrast to the tag scope opened by {@link #makeActive()} this tag scope will reflect
     * all recent updates performed through setData or via up-propagation.
     * In addition, this tag scopes contains all tags for which down-propagation is set to false.
     *
     * @return the newly opened tag scope.
     * // TODO Remove? This becomes obsolete now
     */
    public Scope enterFullTagScope() {
        TagContextBuilder builder = Tags.getTagger().emptyBuilder();
        dataTagsStream().forEach(e -> builder.putLocal(TagKey.create(e.getKey()), TagUtils.createTagValue(e.getKey(), e.getValue()
                .toString())));
        return builder.buildScoped();
    }

    private Stream<Map.Entry<String, Object>> dataTagsStream() {
        return getDataAsStream().filter(e -> propagation.isTag(e.getKey()))
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()));
    }

    /**
     * Returns the most recent value for data, which either was inherited form the parent context,
     * set via {@link #setData(String, Object)} or changed due to an up-propagation.
     *
     * @param key the name of the data to query
     *
     * @return the data element which is related to the given key or `null` if it doesn't exist
     */
    @Override
    public Object getData(String key) {
        if (dataOverwrites.containsKey(key)) {
            return dataOverwrites.get(key);
        } else {
            return postEntryPhaseDownPropagatedData.get(key);
        }
    }

    /**
     * /**
     * Sets the value for a given data key.
     * If this is called during the entry phase of the context, the changed datum will be reflected
     * in postEntryPhaseDownPropagatedData and {@link #getPostEntryPhaseTags()}.
     *
     * @param key   the key of the data to set
     * @param value the value to set
     */
    @Override
    public void setData(String key, Object value) {
        dataOverwrites.put(key, value);
    }

    /**
     * Closes this context.
     * If any {@link TagContext} was opened during {@link #makeActive()}, this context is also closed.
     * In addition up-propagation is performed if this context is not asynchronous.
     */
    @Override
    public void close() {
        if (openedDownPropagationScope != null) {
            openedDownPropagationScope.close();
        }
        Context.current().detach(overriddenGrpcContext);

        if (currentSpanScope != null) {
            try {
                currentSpanScope.close();
            } catch (Throwable e) {
                log.error("Error closing span scope", e);
            }
        }

        if (parent != null && !isInDifferentThreadThanParentOrIsParentClosed()) {
            parent.performUpPropagation(dataOverwrites);
        }
        //clear the references to prevent memory leaks
        openedDownPropagationScope = null;
        currentSpanScope = null;
        parent = null;
        overriddenGrpcContext = null;
    }

    private void performUpPropagation(Map<String, Object> dataWrittenByChild) {
        for (Map.Entry<String, Object> entry : dataWrittenByChild.entrySet()) {
            if (propagation.isPropagatedUpWithinJVM(entry.getKey())) {
                String key = entry.getKey();
                Object value = entry.getValue();
                dataOverwrites.put(key, value);
                if (propagation.isPropagatedDownWithinJVM(key)) {
                    if (propagation.isTag(key)) {
                        isActivePhaseDownPropagationTagContextStale = true;
                    }
                    if (cachedActivePhaseDownPropagatedData != null && cachedActivePhaseDownPropagatedData.get(key) != value) {
                        cachedActivePhaseDownPropagatedData = null;
                    }
                }
            }
        }
    }

    @Override
    public Map<String, String> getDownPropagationHeaders() {
        SpanContext spanContext = Tracing.getTracer().getCurrentSpan().getContext();
        if (!spanContext.isValid()) {
            Object remoteParent = getData(REMOTE_PARENT_SPAN_CONTEXT_KEY);
            if (remoteParent instanceof SpanContext) {
                spanContext = (SpanContext) remoteParent;
            } else {
                spanContext = null;
            }
        }
        return ContextPropagationUtil.buildPropagationHeaderMap(getDataAsStream().filter(e -> propagation.isPropagatedDownGlobally(e
                .getKey())), spanContext);
    }

    @Override
    public Map<String, String> getUpPropagationHeaders() {
        return ContextPropagationUtil.buildPropagationHeaderMap(getDataAsStream().filter(e -> propagation.isPropagatedUpGlobally(e
                .getKey())));
    }

    @Override
    public void readUpPropagationHeaders(Map<String, String> headers) {
        ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, this);
    }

    @Override
    public void readDownPropagationHeaders(Map<String, String> headers) {
        ContextPropagationUtil.readPropagatedDataFromHeaderMap(headers, this);
        SpanContext remote_span = ContextPropagationUtil.readPropagatedSpanContextFromHeaderMap(headers);
        setData(REMOTE_PARENT_SPAN_CONTEXT_KEY, remote_span);
    }

    @Override
    public Set<String> getPropagationHeaderNames() {
        return ContextPropagationUtil.getPropagationHeaderNames();
    }

    /**
     * Only invoked by {@link #createFromCurrent(Map, PropagationMetaData, boolean)}
     * <p>
     * Reads the currently active tag context and makes this context inherit all values which
     * have changed in comparison to the values published by the parent context.
     */
    private void readOverridesFromCurrentTagContext() {
        TagContext currentTags = Tags.getTagger().getCurrentTagContext();
        if (currentTags != null) {
            PropagationMetaData.Builder alteredPropagation = null;
            if (parent == null) {
                //we are the first inspectit context, therefore we inherit all values
                for (Iterator<Tag> it = InternalUtils.getTags(currentTags); it.hasNext(); ) {
                    Tag tag = it.next();
                    setData(tag.getKey().getName(), tag.getValue().asString());
                    alteredPropagation = configureTagPropagation(tag.getKey().getName(), alteredPropagation);
                }
            } else {
                // a new context was opened between our parent and ourselves
                // we look for all values which have changed and inherit them
                if (currentTags != parent.activePhaseDownPropagationTagContext) {
                    for (Iterator<Tag> it = InternalUtils.getTags(currentTags); it.hasNext(); ) {
                        Tag tag = it.next();
                        String tagKey = tag.getKey().getName();
                        String tagValue = tag.getValue().asString();
                        Object parentValueForTag = parent.postEntryPhaseDownPropagatedData.get(tagKey);
                        //only inherit changed values
                        if (parentValueForTag == null || !parentValueForTag.toString().equals(tagValue)) {
                            setData(tagKey, tagValue);
                        }
                        alteredPropagation = configureTagPropagation(tagKey, alteredPropagation);
                    }
                }
            }
            if (alteredPropagation != null) {
                propagation = alteredPropagation.build();
            }
        }
    }

    /**
     * Checks if the given key is already configured in {@link #propagation} for down-propagation and as a tag.
     * If it is the case, the passed in builder is returned without changes.
     * <p>
     * Otherwise the key is configured in the given builder to be down-propagated JVM-locally and to be a tag.
     *
     * @param tagKey          the key of the found tag
     * @param existingBuilder an existing builder to which the settings shall be added. If it is null, a builder is created using copy() on {@link #propagation}.
     *
     * @return exisitingBuilder or the newly created builder if it was null.
     */
    private PropagationMetaData.Builder configureTagPropagation(String tagKey, PropagationMetaData.Builder existingBuilder) {
        PropagationMetaData.Builder result = existingBuilder;
        boolean isTag = propagation.isTag(tagKey);
        boolean isPropagatedDown = propagation.isPropagatedDownWithinJVM(tagKey);
        if (!isTag || !isPropagatedDown) {
            if (result == null) {
                result = propagation.copy();
            }
            result.setTag(tagKey, true);
            if (!isPropagatedDown) {
                result.setDownPropagation(tagKey, PropagationMode.JVM_LOCAL);
            }
        }
        return result;
    }

    private Stream<Map.Entry<String, Object>> getDataAsStream() {
        return Stream.concat(postEntryPhaseDownPropagatedData.entrySet()
                .stream()
                .filter(e -> !dataOverwrites.containsKey(e.getKey())), dataOverwrites.entrySet()
                .stream()
                .filter(e -> e.getValue() != null));
    }

    private Map<String, Object> getOrComputeActivePhaseDownPropagatedData() {
        if (cachedActivePhaseDownPropagatedData == null) {
            cachedActivePhaseDownPropagatedData = getDownPropagatedDataAsNewMap();
        }
        return cachedActivePhaseDownPropagatedData;
    }

    private HashMap<String, Object> getDownPropagatedDataAsNewMap() {
        val result = new HashMap<>(postEntryPhaseDownPropagatedData);

        for (Map.Entry<String, Object> e : dataOverwrites.entrySet()) {
            val key = e.getKey();
            if (propagation.isPropagatedDownWithinJVM(key)) {
                val value = e.getValue();
                if (value != null) {
                    result.put(key, value);
                } else {
                    result.remove(key);
                }
            }
        }

        return result;
    }

    private Iterator<Tag> getPostEntryPhaseTags() {
        return postEntryPhaseDownPropagatedData.entrySet()
                .stream()
                .filter(e -> propagation.isTag(e.getKey()))
                .filter(e -> ALLOWED_TAG_TYPES.contains(e.getValue().getClass()))
                .map(e -> Tag.create(TagKey.create(e.getKey()), TagUtils.createTagValue(e.getKey(), e.getValue()
                        .toString()), TagMetadata.create(TagMetadata.TagTtl.UNLIMITED_PROPAGATION)))
                .iterator();
    }

}
