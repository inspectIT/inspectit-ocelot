package rocks.inspectit.ocelot.core.instrumentation.config.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

/**
 * This event is fired when the {@link InstrumentationConfigurationResolver} has resolved a new {@link InstrumentationConfiguration}.
 * This event usually occurs after a {@link InspectitConfigChangedEvent}.
 * The new {@link InspectitConfig} is used to derive the {@link InstrumentationConfiguration},
 * for which then this event is triggered.
 */
@Getter
public class InstrumentationConfigurationChangedEvent extends ApplicationEvent {

    /**
     * The configuration which has previously been active.
     */
    private final InstrumentationConfiguration oldConfig;

    /**
     * The configuration which just has become active.
     * This is equal to the config returned by {@link InstrumentationConfigurationResolver#getCurrentConfig()}.
     */
    private final InstrumentationConfiguration newConfig;

    public InstrumentationConfigurationChangedEvent(Object source, InstrumentationConfiguration oldConfig, InstrumentationConfiguration newConfig) {
        super(source);
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
    }
}
