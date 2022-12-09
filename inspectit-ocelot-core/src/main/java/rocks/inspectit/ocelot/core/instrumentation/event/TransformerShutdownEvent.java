package rocks.inspectit.ocelot.core.instrumentation.event;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.core.instrumentation.AsyncClassTransformer;

/**
 * An event sent to notify listeners that the {@link AsyncClassTransformer} is shutting down
 */
public class TransformerShutdownEvent extends ApplicationEvent {
    /**
     * Create a new TransformerShutdownEvent.
     *
     * @param source the transformer which is shutting down
     */
    public TransformerShutdownEvent(AsyncClassTransformer source) {
        super(source);
    }
}
