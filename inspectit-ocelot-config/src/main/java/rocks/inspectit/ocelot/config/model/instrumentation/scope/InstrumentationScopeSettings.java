package rocks.inspectit.ocelot.config.model.instrumentation.scope;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Data container for settings which are used as basis for {@link rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope}.
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
    @Valid
    @NotNull
    private List<ElementDescriptionMatcherSettings> interfaces = Collections.emptyList();

    /**
     * Superclass which has to be extended.
     */
    @Valid
    private ElementDescriptionMatcherSettings superclass;

    /**
     * Matcher which have to match the type's name.
     */
    @Valid
    private ElementDescriptionMatcherSettings type;

    /**
     * Scopes which are to be excluded
     */
    @Valid
    private Map<String, Boolean> exclude = Collections.emptyMap();

    /**
     * Defines which methods are targeted by this scope.
     */
    @Valid
    @NotNull
    private List<MethodMatcherSettings> methods = Collections.emptyList();

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

        return !CollectionUtils.isEmpty(interfaces)
                || superclass != null
                || (type != null && !type.isAnyMatcher());
    }

    /**
     * Validates this scope, invoked by {@link InstrumentationSettings#performValidation(InspectitConfig, ViolationBuilder)}
     *
     * @param container      the root config containing this rule
     * @param vios           the violation builder
     */
    public void performValidation(String name, InstrumentationSettings container, ViolationBuilder vios, Set<String> verified) {
        // Verify that the excluded scopes have also been defined.
        exclude.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .filter(excludeName -> !container.getScopes().containsKey(excludeName))
                .forEach(excludeName -> vios.message("Specified excluded scope '{scope}' was not defined!")
                        .atProperty("scopes")
                        .parameter("scope", excludeName)
                        .buildAndPublish());

        ArrayList<String> visitedParents = new ArrayList<>();
        visitedParents.add(name);
        verifyCyclicalDependence(name, container.getScopes(), verified, visitedParents, vios);

    }

    /**
     * Verify that there are no cyclic dependencies between the excluded scopes.
     *
     * @param parentScope the scope, which holds the exclude-scopes
     * @param scopes map of scopes
     * @param verified a list containing already verified scopes
     * @param visitedParents temp list for verified parentScopes
     * @param vios the violation output
     */
    private static void verifyCyclicalDependence(String parentScope, Map<String, InstrumentationScopeSettings> scopes, Set<String> verified, List<String> visitedParents, ViolationBuilder vios) {
        if(verified.contains(parentScope)){
            return;
        }

        scopes.get(parentScope).getExclude().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .forEach(excludeScope -> {
                    if (visitedParents.contains(excludeScope)) {
                        vios.message("Specified excluded Scope in '{scope}' has cyclical dependence with other Scope -> '{excludeScope}'") // verifiedParens ausgeben
                                .atProperty("scopes")
                                .parameter("scope", parentScope)
                                .parameter("excludeScope", excludeScope)
                                .buildAndPublish();
                    }
                    else {
                        visitedParents.add(excludeScope);
                        if (scopes.containsKey(excludeScope)) {
                            verifyCyclicalDependence(excludeScope, scopes, verified, visitedParents, vios);
                        }
                        visitedParents.remove(visitedParents.size() - 1);
                    }
                });
        verified.add(parentScope);
    }
}