package rocks.inspectit.oce.core.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.AssertTrue;
import java.util.List;

/**
 * Data container for settings which are used as basis for {@link rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class InstrumentationScopeSettings {

    /**
     * Defines which classes are targeted by this scope.
     */
    private TypeScope typeScope;

    /**
     * Defines which methods are targeted by this scope.
     */
    private List<MethodMatcherSettings> methodScope;

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

        List<NameMatcherSettings> interfaces = typeScope.getInterfaces();
        NameMatcherSettings superclass = typeScope.getSuperclass();
        List<NameMatcherSettings> classes = typeScope.getTypes();

        // No scope
        if (typeScope == null) {
            return false;
        }
        // No super classes and...
        if (CollectionUtils.isEmpty(interfaces) && superclass == null) {
            // ...no classes
            if (CollectionUtils.isEmpty(classes)) {
                return false;
            }

            // ...any-matcher
            boolean anyMatch = classes.stream().anyMatch(NameMatcherSettings::isAnyMatcher);
            if (anyMatch) {
                return false;
            }
        }
        return true;
    }

}