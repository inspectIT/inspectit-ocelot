package rocks.inspectit.oce.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.NameMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.TypeScope;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.config.util.MatcherChainBuilder;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static rocks.inspectit.oce.core.instrumentation.config.util.SpecialElementMatchers.*;

/**
 * This class is used to resolve the {@link InstrumentationScope} based on the {@link InstrumentationScopeSettings} contained
 * in the configuration.
 */
@Component
@Slf4j
public class InstrumentationScopeResolver {

    /**
     * Creates the {@link InstrumentationScope} instances based on the {@link InstrumentationScopeSettings} contained in
     * the given {@link InstrumentationSettings}.
     *
     * @param source the configuration used as basis for the {@link InstrumentationScope}s
     * @return Returns a map containing {@link InstrumentationScope} instances. The keys are representing a unique identifier for each scope.
     */
    public Map<String, InstrumentationScope> resolve(InstrumentationSettings source) {
        Map<String, InstrumentationScope> scopeMap = new HashMap<>();

        if (source.getScopes() != null) {
            for (Map.Entry<String, InstrumentationScopeSettings> scopeEntry : source.getScopes().entrySet()) {
                InstrumentationScopeSettings scopeSettings = scopeEntry.getValue();

                if (log.isDebugEnabled()) {
                    log.debug("Processing scope '{}'.", scopeEntry.getKey());
                }

                InstrumentationScope scope = resolveScope(scopeSettings);

                if (log.isDebugEnabled()) {
                    log.debug("|> Type scope: {}", scope.getTypeMatcher().toString());
                    log.debug("|> Method scope: {}", scope.getMethodMatcher().toString());
                }

                scopeMap.put(scopeEntry.getKey(), scope);
            }
        }

        return scopeMap;
    }

    /**
     * Resolving the {@link InstrumentationScope} for the given parameters.
     *
     * @return The resolve {@link InstrumentationScope}.
     */
    private InstrumentationScope resolveScope(InstrumentationScopeSettings scopeSettings) {
        ElementMatcher.Junction<TypeDescription> typeMatcher = buildTypeMatcher(scopeSettings);
        ElementMatcher.Junction<MethodDescription> methodMatcher = buildMethodMatcher(scopeSettings, typeMatcher);

        if (typeMatcher == null) {
            typeMatcher = any();
        }
        if (methodMatcher == null) {
            methodMatcher = any();
        }

        return new InstrumentationScope(typeMatcher, methodMatcher);
    }

    /**
     * Creates the {@link ElementMatcher} which is matching the scope's type specified in the configuration.
     */
    private ElementMatcher.Junction<TypeDescription> buildTypeMatcher(InstrumentationScopeSettings scopeSettings) {
        if (scopeSettings.getTypeScope() == null) {
            return null;
        }

        MatcherChainBuilder<TypeDescription> builder = new MatcherChainBuilder<>();

        if (scopeSettings.getTypeScope().getSuperclass() != null) {
            processSuperclass(builder, scopeSettings.getTypeScope().getSuperclass());
        }
        if (scopeSettings.getTypeScope().getInterfaces() != null) {
            scopeSettings.getTypeScope().getInterfaces().forEach(i -> processInterfaces(builder, i));
        }
        if (scopeSettings.getTypeScope().getTypes() != null) {
            scopeSettings.getTypeScope().getTypes().forEach(c -> processType(builder, c));
        }

        if (scopeSettings.getAdvanced() != null && scopeSettings.getAdvanced().isInstrumentOnlyAbstractClasses()) {
            builder.and(isAbstract());
        }

        return builder.build();
    }

    /**
     * Processing the interface and superclass settings of the {@link TypeScope}.
     */
    private void processSuperclass(MatcherChainBuilder<TypeDescription> builder, NameMatcherSettings nameSettings) {
        ElementMatcher.Junction<NamedElement> nameMatcher = nameIs(nameSettings);
        if (nameMatcher != null) {
            builder.and(hasSuperType(not(isInterface()).and(nameMatcher)));
        }
    }

    /**
     * Processing the interface and superclass settings of the {@link TypeScope}.
     */
    private void processInterfaces(MatcherChainBuilder<TypeDescription> builder, NameMatcherSettings nameSettings) {
        ElementMatcher.Junction<NamedElement> nameMatcher = nameIs(nameSettings);
        if (nameMatcher != null) {
            builder.and(hasSuperType(isInterface().and(nameMatcher)));
        }
    }

    /**
     * Processing the class settings of the {@link TypeScope}.
     */
    private void processType(MatcherChainBuilder<TypeDescription> builder, NameMatcherSettings nameSettings) {
        ElementMatcher.Junction<TypeDescription> nameMatcher = nameIs(nameSettings);
        if (nameMatcher != null) {
            builder.and(nameMatcher);
        }
    }

    /**
     * Creating an {@link ElementMatcher} which matches the specified methods.
     */
    private ElementMatcher.Junction<MethodDescription> buildMethodMatcher(InstrumentationScopeSettings scopeSettings, ElementMatcher.Junction<TypeDescription> typeMatcher) {
        MatcherChainBuilder<MethodDescription> builder = new MatcherChainBuilder<>();

        if (scopeSettings.getMethodScope() != null) {
            scopeSettings.getMethodScope().forEach(m -> processMethod(builder, m));
        }

        if (scopeSettings.getAdvanced() != null && scopeSettings.getAdvanced().isInstrumentOnlyInheritedMethods()) {
            ElementMatcher.Junction<MethodDescription> overrideMatcher = onlyOverridenMethodsOf(scopeSettings.getTypeScope());
            if (overrideMatcher != null) {
                builder.and(overrideMatcher);
            }
        }

        return builder.build();
    }

    /**
     * Processing a single {@link MethodMatcherSettings} and adding it to the given {@link MatcherChainBuilder}.
     */
    private void processMethod(MatcherChainBuilder<MethodDescription> builder, MethodMatcherSettings matcherSettings) {
        MatcherChainBuilder<MethodDescription> innerBuilder = new MatcherChainBuilder<>();

        innerBuilder.and(matcherSettings.isConstructor(), isConstructor());
        innerBuilder.and(visibilityIs(matcherSettings.getVisibility()));
        innerBuilder.and(argumentsAre(matcherSettings.getArguments()));

        if (!matcherSettings.isConstructor()) {
            innerBuilder.and(nameIs(matcherSettings));

            if (matcherSettings.getIsSynchronized() != null) {
                innerBuilder.and(matcherSettings.getIsSynchronized(), isSynchronized());
            }
        }

        builder.or(innerBuilder.build());
    }
}
