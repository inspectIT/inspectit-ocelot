package rocks.inspectit.ocelot.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * Data container for settings which will be used as basis for the {@link rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope}
 * and its method matcher.
 * <p>
 * An instance of this class represents a matcher which is matching a set of methods.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MethodMatcherSettings extends ElementDescriptionMatcherSettings {

    /**
     * Enum for access modifiers which can be used in the instrumentation configuration.
     */
    public enum AccessModifier {
        PUBLIC, PROTECTED, PACKAGE, PRIVATE
    }

    /**
     * Whether the method is a constructor or not.
     */
    @NotNull
    private Boolean isConstructor = false;

    /**
     * Whether the method is synchronized or not. If this property is `null` the synchronize keyword will be ignored.
     */
    private Boolean isSynchronized;

    /**
     * The arguments which have to match the method's signature.
     */
    private List<String> arguments;

    /**
     * The methods visibility. On of the specified matcher has to match.
     */
    @NotNull
    private List<AccessModifier> visibility = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PROTECTED, AccessModifier.PACKAGE, AccessModifier.PRIVATE);
}
