package rocks.inspectit.ocelot.core.instrumentation.event;

import lombok.Getter;
import net.bytebuddy.description.type.TypeDescription;
import org.springframework.context.ApplicationEvent;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;

/**
 * Fired by a {@link rocks.inspectit.ocelot.core.instrumentation.transformer.ClassTransformer} whenever a class has been instrumented or deinstrumented.
 * Note that this event is executed at the end of the {@link java.lang.instrument.ClassFileTransformer}.
 * This means that performed instrumentation is not active at the time this event is fired.
 */
public class ClassInstrumentedEvent extends ApplicationEvent {

    /**
     * The class which has been instrumented.
     */
    @Getter
    private final Class<?> instrumentedClass;

    /**
     * The description for the class which has been instrumented.
     */
    @Getter
    private final TypeDescription instrumentedClassDescription;

    /**
     * The instrumentation which has been applied.
     * If no instrumentation was applied or the instrumentation has been removed this configuration
     * is the same as {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION}.
     */
    @Getter
    private final ClassInstrumentationConfiguration appliedConfiguration;

    public ClassInstrumentedEvent(Object source, Class<?> instrumentedClass, TypeDescription instrumentedClassDescription, ClassInstrumentationConfiguration appliedConfiguration) {
        super(source);
        this.instrumentedClass = instrumentedClass;
        this.instrumentedClassDescription = instrumentedClassDescription;
        this.appliedConfiguration = appliedConfiguration;
    }
}
