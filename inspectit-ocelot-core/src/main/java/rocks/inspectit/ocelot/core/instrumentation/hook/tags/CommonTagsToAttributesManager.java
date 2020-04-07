package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import javax.annotation.PostConstruct;
import java.util.Objects;

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
     * Action implementation.
     */
    @Getter
    private CommonTagsToAttributesAction action;

    /**
     * Default constructor.
     */
    public CommonTagsToAttributesManager(InspectitEnvironment env, CommonTagsManager commonTagsManager) {
        this.env = env;
        this.commonTagsManager = commonTagsManager;
        this.action = new CommonTagsToAttributesAction(commonTagsManager, TracingSettings.AddCommonTags.NEVER);
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
        if (!Objects.equals(tracing.getAddTags(), action.getAddCommonTags())) {
            this.action = new CommonTagsToAttributesAction(commonTagsManager, tracing.getAddTags());
        }
    }

}
