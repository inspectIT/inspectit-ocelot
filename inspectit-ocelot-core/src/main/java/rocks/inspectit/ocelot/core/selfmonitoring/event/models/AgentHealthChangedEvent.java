package rocks.inspectit.ocelot.core.selfmonitoring.event.models;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.selfmonitoring.logs.LogPreloader;

/**
 * Fired by {@link LogPreloader} whenever the agent health changed.
 */
public class AgentHealthChangedEvent extends ApplicationEvent {

    /**
     * The health the agent had before it changed.
     */
    @Getter
    private final AgentHealth oldHealth;

    /**
     * The health the agent has after the change.
     */
    @Getter
    private AgentHealth newHealth;

    /**
     * The message stating the cause of the event.
     */
    @Getter
    private String message;

    /**
     * Indicates if this event caused a health change.
     */
    private boolean changedState;

    public AgentHealthChangedEvent(Object source, @NonNull AgentHealth oldHealth, @NonNull AgentHealth newHealth, String message) {
        super(source);
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
        this.message = message;
    }

}
