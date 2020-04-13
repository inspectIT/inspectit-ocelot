package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * This class is used when creating spans to determine if the common tags should be added to the created span or not.
 */
@Component
public class CommonTagsToAttributesManager {

    /**
     * Environment.
     */
    private final InspectitEnvironment env;

    /**
     * Common tags manager.
     */
    private final CommonTagsManager commonTagsManager;

    /**
     * Currently active setting.
     */
    private TracingSettings.AddCommonTags addCommonTags;

    /**
     * Default constructor.
     */
    @Autowired
    public CommonTagsToAttributesManager(InspectitEnvironment env, CommonTagsManager commonTagsManager) {
        this.env = env;
        this.commonTagsManager = commonTagsManager;
        this.addCommonTags = TracingSettings.AddCommonTags.NEVER;
    }

    /**
     * Creates the new #action based on the current config.
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @PostConstruct
    @VisibleForTesting
    void update() {
        InspectitConfig configuration = env.getCurrentConfig();
        TracingSettings tracing = configuration.getTracing();
        if (!Objects.equals(tracing.getAddCommonTags(), addCommonTags)) {
            this.addCommonTags = tracing.getAddCommonTags();
        }
    }

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
