package rocks.inspectit.ocelot.core.selfmonitoring.event;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.commons.models.health.AgentHealth;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;

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

    public AgentHealthChangedEvent(Object source, @NonNull AgentHealth oldHealth, @NonNull AgentHealth newHealth) {
        super(source);
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
    }

}
