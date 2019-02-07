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
import rocks.inspectit.oce.core.instrumentation.config.matcher.MatcherChainBuilder;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static rocks.inspectit.oce.core.instrumentation.config.matcher.SpecialElementMatchers.*;

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

        for (Map.Entry<String, InstrumentationScopeSettings> scopeEntry : source.getScopes().entrySet()) {
            InstrumentationScopeSettings scopeSettings = scopeEntry.getValue();

            log.debug("Processing scope '{}'.", scopeEntry.getKey());

            InstrumentationScope scope = resolveScope(scopeSettings);

            if (log.isDebugEnabled()) {
                log.debug("|> Type scope: {}", scope.getTypeMatcher().toString());
                log.debug("|> Method scope: {}", scope.getMethodMatcher().toString());
            }

            scopeMap.put(scopeEntry.getKey(), scope);
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
        MatcherChainBuilder<TypeDescription> builder = new MatcherChainBuilder<>();

        if (scopeSettings.getSuperclass() != null) {
            processSuperclass(builder, scopeSettings.getSuperclass());
        }
        if (scopeSettings.getInterfaces() != null) {
            scopeSettings.getInterfaces().forEach(i -> processInterface(builder, i));
        }
        if (scopeSettings.getType() != null) {
            processType(builder, scopeSettings.getType());
        }

        return builder.build();
    }

    /**
     * Creating a matcher for the given {@link NameMatcherSettings} which represents a superclass.
     */
    private void processSuperclass(MatcherChainBuilder<TypeDescription> builder, NameMatcherSettings nameSettings) {
        ElementMatcher.Junction<NamedElement> nameMatcher = nameIs(nameSettings);
        if (nameMatcher != null) {
            builder.and(hasSuperType(not(isInterface()).and(nameMatcher)));
        }
    }

    /**
     * Creating a matcher for the given {@link NameMatcherSettings} which represents an interface.
     */
    private void processInterface(MatcherChainBuilder<TypeDescription> builder, NameMatcherSettings nameSettings) {
        ElementMatcher.Junction<NamedElement> nameMatcher = nameIs(nameSettings);
        if (nameMatcher != null) {
            builder.and(hasSuperType(isInterface().and(nameMatcher)));
        }
    }

    /**
     * Creating a matcher for the given {@link NameMatcherSettings} which represents concrete classes.
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

        if (scopeSettings.getMethods() != null) {
            scopeSettings.getMethods().forEach(m -> processMethod(builder, m));
        }

        if (scopeSettings.getAdvanced() != null && scopeSettings.getAdvanced().isInstrumentOnlyInheritedMethods()) {
            ElementMatcher.Junction<MethodDescription> overrideMatcher = onlyOverridenMethodsOf(scopeSettings);
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

        innerBuilder.and(matcherSettings.getIsConstructor(), isConstructor());
        innerBuilder.and(visibilityIs(matcherSettings.getVisibility()));
        innerBuilder.and(argumentsAre(matcherSettings.getArguments()));

        if (!matcherSettings.getIsConstructor()) {
            innerBuilder.and(nameIs(matcherSettings));

            if (matcherSettings.getIsSynchronized() != null) {
                innerBuilder.and(matcherSettings.getIsSynchronized(), isSynchronized());
            }
        }

        builder.or(innerBuilder.build());
    }
}
