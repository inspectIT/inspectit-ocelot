package rocks.inspectit.ocelot.core.instrumentation.event;

import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.core.instrumentation.transformer.ClassTransformer;

/**
 * An event sent to notify listeners that a {@link ClassTransformer} is shutting down
 */
public class TransformerShutdownEvent extends ApplicationEvent {

    /**
     * Create a new TransformerShutdownEvent.
     *
     * @param source the transformer which is shutting down
     */
    public TransformerShutdownEvent(ClassTransformer source) {
        super(source);
    }
}
