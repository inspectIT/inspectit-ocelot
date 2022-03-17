package rocks.inspectit.ocelot.core.selfmonitoring.event;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.commons.models.status.AgentStatus;
import rocks.inspectit.ocelot.core.selfmonitoring.LogPreloader;

import java.util.Optional;

/**
 * Fired by {@link LogPreloader} whenever the agent status changed.
 */
public class AgentStatusChangedEvent extends ApplicationEvent {

    /**
     * The state the agent had before it changed.
     */
    @Getter
    private final Optional<AgentStatus> oldStatus;

    /**
     * The state the agent has after the change.
     */
    @Getter
    private AgentStatus newStatus;

    public AgentStatusChangedEvent(Object source, @NonNull Optional<AgentStatus> oldStatus, @NonNull AgentStatus newStatus) {
        super(source);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

}
