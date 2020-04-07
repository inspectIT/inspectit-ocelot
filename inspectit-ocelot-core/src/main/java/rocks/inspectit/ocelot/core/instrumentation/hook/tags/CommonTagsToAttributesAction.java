package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

@Value
@Getter(AccessLevel.PACKAGE)
public class CommonTagsToAttributesAction {

    /**
     * Tags manager.
     */
    private final CommonTagsManager commonTagsManager;

    /**
     * Currently active setting.
     */
    private final TracingSettings.AddCommonTags addCommonTags;

    /**
     * Writes common tags to span depending on the current {@link #addCommonTags} setting and the provided information about the span.
     *
     * @param span            Span
     * @param hasRemoteParent If span has remote parent
     * @param hasLocalParent  If span has local parent
     */
    public void writeCommonTags(Span span, boolean hasRemoteParent, boolean hasLocalParent) {
        if (shouldAdd(hasRemoteParent, hasLocalParent)) {
            commonTagsManager.getCommonTagValueMap()
                    .forEach((k, v) -> span.putAttribute(k, AttributeValue.stringAttributeValue(v)));
        }
    }

    /**
     * If tags should be added.
     */
    private boolean shouldAdd(boolean hasRemoteParent, boolean hasLocalParent) {
        switch (addCommonTags) {
            case ALWAYS:
                return true;
            case ON_LOCAL_ROOT:
                return !hasLocalParent;
            case ON_GLOBAL_ROOT:
                return !hasRemoteParent && !hasLocalParent;
            default:
                return false;
        }
    }

}
