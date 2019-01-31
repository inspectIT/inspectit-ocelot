package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data container for settings which will be used as basis for the {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}
 * and its method matcher.
 * <p>
 * An instance of this class represents a matcher which is matching a set of methods.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MethodMatcherSettings extends NameMatcherSettings {

    /**
     * Enum for access modifiers which can be used in the instrumentation configuration.
     */
    public enum AccessModifier {
        PUBLIC, PROTECTED, PACKAGE, PRIVATE
    }

    /**
     * Whether the method is a constructor or not.
     */
    private Boolean isConstructor = false;

    /**
     * Whether the method is synchronized or not.
     */
    private Boolean isSynchronized;

    /**
     * The arguments which have to match the method's signature.
     */
    private String[] arguments;

    /**
     * The methods visibility. On of the specified matcher has to match.
     */
    private AccessModifier[] visibility;
}
