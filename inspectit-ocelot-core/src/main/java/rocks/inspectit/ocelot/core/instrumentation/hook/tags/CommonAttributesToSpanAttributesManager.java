package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * This class is used when creating spans to determine if the common attributes should be added to the created span or not.
 */
@Component
public class CommonAttributesToSpanAttributesManager {

    private final InspectitEnvironment env;

    /**
     * Common tags manager.
     */
    private final CommonAttributesManager commonAttributes;

    /**
     * Currently active setting.
     */
    private TracingSettings.AddCommonAttributes addCommonAttributes;

    /**
     * Default constructor.
     */
    @Autowired
    public CommonAttributesToSpanAttributesManager(InspectitEnvironment env, CommonAttributesManager commonAttributes) {
        this.env = env;
        this.commonAttributes = commonAttributes;
        addCommonAttributes = TracingSettings.AddCommonAttributes.NEVER;
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
        if (!Objects.equals(tracing.getAddCommonAttributes(), addCommonAttributes)) {
            addCommonAttributes = tracing.getAddCommonAttributes();
        }
    }

    /**
     * Writes common attributes to span depending on the current {@link #addCommonAttributes} setting
     * and the provided information about the span.
     *
     * @param span            Span
     * @param hasRemoteParent If span has remote parent
     * @param hasLocalParent  If span has local parent
     */
    public void writeCommonAttributes(Span span, boolean hasRemoteParent, boolean hasLocalParent) {
        if (shouldAdd(hasRemoteParent, hasLocalParent)) {
            commonAttributes.getCommonAttributeValueMap().forEach((k, v) -> span.setAttribute(AttributeKey.stringKey(k), v));
        }
    }

    /**
     * If tags should be added.
     */
    private boolean shouldAdd(boolean hasRemoteParent, boolean hasLocalParent) {
        switch (addCommonAttributes) {
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
