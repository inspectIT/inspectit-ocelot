package rocks.inspectit.oce.core.config;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.oce.core.config.model.InspectitConfig;

public class InspectitConfigChangedEvent extends ApplicationEvent {

    /**
     * The configuration before the change occured.
     */
    @Getter
    private final InspectitConfig oldConfig;

    /**
     * The configuration after the change occured. This is the same configuration also returned by {@link InspectitEnvironment#getCurrentConfig()}.
     */
    @Getter
    private final InspectitConfig newConfig;


    InspectitConfigChangedEvent(Object source, InspectitConfig oldConfig, InspectitConfig newConfig) {
        super(source);
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
    }
}
