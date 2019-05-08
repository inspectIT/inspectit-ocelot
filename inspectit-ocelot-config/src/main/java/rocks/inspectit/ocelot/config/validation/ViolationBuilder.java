package rocks.inspectit.ocelot.config.validation;

import java.util.Collection;
import java.util.Objects;

/**
 * Builder for violations.
 * In contrast to other Builder the methods do not alter the state of this builder, but instead construct a new builder with the adapted state.
 */
public class ViolationBuilder {

    private final Collection<? super Violation> sink;

    private Violation current;

    public ViolationBuilder(Collection<? super Violation> sink) {
        this.sink = sink;
        current = new Violation.ViolationBuilder().build();
    }

    /**
     * Only visible for testing
     *
     * @param sink
     * @param base
     */
    ViolationBuilder(Collection<? super Violation> sink, Violation base) {
        this.sink = sink;
        current = base;
    }

    /**
     * @param message the message of the violation, parameters use the syntax {parameter}
     * @return the new builder
     */
    public ViolationBuilder message(String message) {
        return new ViolationBuilder(sink, current.toBuilder().message(message).build());
    }

    /**
     * Defines a parameter value referenced in the {@link #message(String)}
     *
     * @param name  the name of the parameter
     * @param value its value
     * @return the newly created builder
     */
    public ViolationBuilder parameter(String name, Object value) {
        return new ViolationBuilder(sink, current.toBuilder()
                .parameter(name, Objects.toString(value))
                .build());
    }

    /**
     * Defines that the violation was found at the given property.
     * The invocation of this method can be chained to form a path, e.g.
     * atProperty("my").atProperty("attib") results in the property path "my.attribute".
     *
     * @param name the name of the property
     * @return the newly created builder
     */
    public ViolationBuilder atProperty(String name) {
        return new ViolationBuilder(sink, current.toBuilder()
                .beanNode(name)
                .build());
    }

    /**
     * Finishes the building of this violation and publishes it, so that it is returned as a {@link javax.validation.ConstraintViolation}
     * by the validator.
     */
    public void buildAndPublish() {
        sink.add(current);
    }

}
