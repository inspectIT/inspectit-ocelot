package rocks.inspectit.ocelot.core.config;

import org.springframework.context.ApplicationEvent;

/**
 * This event is fired whenever the set of {@link org.springframework.context.annotation.PropertySource}s has changed.
 * In contrast to {@link InspectitConfigChangedEvent} this event also fires if this does not cause a change
 * of the resulting {@link rocks.inspectit.ocelot.config.model.InspectitConfig}.
 */
public class PropertySourcesChangedEvent extends ApplicationEvent {

    PropertySourcesChangedEvent(Object source) {
        super(source);
    }
}
