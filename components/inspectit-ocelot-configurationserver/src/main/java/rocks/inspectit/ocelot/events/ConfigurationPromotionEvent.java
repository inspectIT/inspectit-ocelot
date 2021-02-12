package rocks.inspectit.ocelot.events;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

/**
 * This event is fired when configurations have been promoted.
 */
public class ConfigurationPromotionEvent extends ApplicationEvent {

    private final RevisionAccess liveRevision;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public ConfigurationPromotionEvent(Object source, RevisionAccess liveRevision) {
        super(source);
        this.liveRevision = liveRevision;
    }

    /**
     * @return the new live revision after the promotion.
     */
    public RevisionAccess getLiveRevision() {
        return liveRevision;
    }
}
