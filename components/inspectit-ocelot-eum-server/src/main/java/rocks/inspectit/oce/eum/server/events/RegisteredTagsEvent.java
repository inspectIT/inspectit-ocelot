package rocks.inspectit.oce.eum.server.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.Set;

public class RegisteredTagsEvent extends ApplicationEvent {

    @Getter
    private final Set<String> registeredTags;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public RegisteredTagsEvent(Object source, Set<String> registeredTags) {
        super(source);
        this.registeredTags = Collections.unmodifiableSet(registeredTags);
    }
}
