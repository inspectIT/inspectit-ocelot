package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;

public class ConfigurationPromotionEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public ConfigurationPromotionEvent(Object source) {
        super(source);
    }
}
