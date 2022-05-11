package rocks.inspectit.ocelot.core.config;

import org.springframework.context.ApplicationEvent;

/**
 * This event is fired whenever the set of {@link org.springframework.context.annotation.PropertySource}s is to be reloaded.
 * In contrast to {@link PropertySourcesChangedEvent}, this event fires <b>before</b> the reload process.
 */
public class PropertySourcesReloadEvent extends ApplicationEvent {

    PropertySourcesReloadEvent(Object source) {
        super(source);
    }
}
