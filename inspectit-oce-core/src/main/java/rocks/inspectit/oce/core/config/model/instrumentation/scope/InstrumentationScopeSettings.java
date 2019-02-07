package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.AssertTrue;
import java.util.List;

/**
 * Data container for settings which are used as basis for {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}.
 * Instances of this class will result in a matcher specifying which types and methods are targeted by an instrumentation.
 * <p>
 * Note: the conjunction of all defined matchers in interfaces, superclass and type will be used to for matching the type.
 * The disjunction of the defined matchers in methods is used for targeting methods.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationScopeSettings {

    /**
     * Interfaces which have to be implemented.
     */
    private List<NameMatcherSettings> interfaces;

    /**
     * Superclass which has to be extended.
     */
    private NameMatcherSettings superclass;

    /**
     * Matcher which have to match the type's name.
     */
    private NameMatcherSettings type;

    /**
     * Defines which methods are targeted by this scope.
     */
    private List<MethodMatcherSettings> methods;

    /**
     * The scope's advanced settings.
     */
    private AdvancedScopeSettings advanced;

    /**
     * Returns whether this scope will is narrowing the target type-scope, thus, is not matching on all types (e.g. using ".*" regular expression).
     *
     * @return Returns true if the scope is narrowing the target type-scope.
     */
    @AssertTrue(message = "The defined scope is not narrowing the type-scope, thus, matching ANY type! To prevent performance issues, the configuration is rejected. You can enforce using this by setting the 'disable-safety-mechanisms' property.")
    public boolean isNarrowScope() {
        if (advanced != null && advanced.isDisableSafetyMechanisms()) {
            return true;
        }

        return !CollectionUtils.isEmpty(interfaces) || superclass != null || (type != null && !type.isAnyMatcher());
    }
}