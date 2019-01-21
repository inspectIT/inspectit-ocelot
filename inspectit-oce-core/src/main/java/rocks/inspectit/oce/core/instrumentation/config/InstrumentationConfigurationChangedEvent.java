package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * This event is fired when the {@link InstrumentationConfigurationResolver} has resolved a new {@link InstrumentationConfiguration}.
 * This event usually occurs after a {@link rocks.inspectit.oce.core.config.InspectitConfigChangedEvent}.
 * The new {@link rocks.inspectit.oce.core.config.model.InspectitConfig} is used to derive the {@link InstrumentationConfiguration},
 * for which then this event is triggered.
 */

public class InstrumentationConfigurationChangedEvent extends ApplicationEvent {

    /**
     * The configuration which has previously been active.
     */
    @Getter
    private final InstrumentationConfiguration oldConfig;

    /**
     * The configuration which just has become active.
     * This is equal to the config returned by {@link InstrumentationConfigurationResolver#getCurrentConfig()}.
     */
    @Getter
    private final InstrumentationConfiguration newConfig;

    InstrumentationConfigurationChangedEvent(Object source, InstrumentationConfiguration oldConfig, InstrumentationConfiguration newConfig) {
        super(source);
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
    }
}
