package rocks.inspectit.ocelot.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MethodMatcherSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.NameMatcherSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.matcher.MatcherChainBuilder;
import rocks.inspectit.ocelot.core.instrumentation.config.matcher.SpecialElementMatchers;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

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

        for (String scopeName : source.getScopes().keySet()) {
            log.debug("Processing scope '{}'.", scopeName);

            resolveScope(scopeName, source.getScopes(), scopeMap);

            if (log.isDebugEnabled()) {
                log.debug("|> Type scope: {}", scopeMap.get(scopeName).getTypeMatcher().toString());
                log.debug("|> Method scope: {}", scopeMap.get(scopeName).getMethodMatcher().toString());
            }

        }

        return scopeMap;
    }

    /**
     * Resolving the {@link InstrumentationScope} for the given parameters and extending the {@param cache} map.
     *
     * @param name of scope to be resolved
     * @param settings a map containing the {@link InstrumentationSettings} scopes
     * @param cache a map containing the resolved scopes, which will be extended
     */
    private void resolveScope(String name, Map<String, InstrumentationScopeSettings> settings, Map<String, InstrumentationScope> cache) {

        if(cache.containsKey(name)){
            return;
        }

        InstrumentationScopeSettings scopeSettings = settings.get(name);
        ElementMatcher.Junction<TypeDescription> typeMatcher = buildTypeMatcher(scopeSettings);
        ElementMatcher.Junction<MethodDescription> methodMatcher = buildMethodMatcher(scopeSettings, typeMatcher);

        if (typeMatcher == null) {
            typeMatcher = any();
        }
        if (methodMatcher == null) {
            methodMatcher = any();
        }

        // resolving of the specified excluded scopes
        for(Map.Entry<String, Boolean> entry:scopeSettings.getExclude().entrySet()){
            if(entry.getValue()) {
                String excludeScopeName = entry.getKey();
                resolveScope(excludeScopeName, settings, cache);

                InstrumentationScope excludeScope = cache.get(excludeScopeName);
                methodMatcher = methodMatcher.and(not(isDeclaredBy(excludeScope.getTypeMatcher()).and(excludeScope.getMethodMatcher())));
            }
        }

        // we ensure that we only match types which contain at least one matched method
        typeMatcher = typeMatcher.and(declaresMethod(methodMatcher));

        cache.put(name, new InstrumentationScope(typeMatcher, methodMatcher));
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
     * Creating and adding a matcher for the given {@link NameMatcherSettings} which represents a superclass.
     */
    private void processSuperclass(MatcherChainBuilder<TypeDescription> builder, ElementDescriptionMatcherSettings descriptionSettings) {
        ElementMatcher.Junction<TypeDescription> matcher = SpecialElementMatchers.describedBy(descriptionSettings);
        if (matcher != null) {
            builder.and(hasSuperType(not(isInterface()).and(matcher)));
        }
    }

    /**
     * Creating and adding a matcher for the given {@link NameMatcherSettings} which represents an interface.
     */
    private void processInterface(MatcherChainBuilder<TypeDescription> builder, ElementDescriptionMatcherSettings descriptionSettings) {
        ElementMatcher.Junction<TypeDescription> matcher = SpecialElementMatchers.describedBy(descriptionSettings);
        if (matcher != null) {
            builder.and(hasSuperType(isInterface().and(matcher)));
        }
    }

    /**
     * Creating and adding a matcher for the given {@link NameMatcherSettings} which represents concrete classes.
     */
    private void processType(MatcherChainBuilder<TypeDescription> builder, ElementDescriptionMatcherSettings descriptionSettings) {
        ElementMatcher.Junction<TypeDescription> matcher = SpecialElementMatchers.describedBy(descriptionSettings);
        builder.and(matcher);
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
            ElementMatcher.Junction<MethodDescription> overrideMatcher = SpecialElementMatchers.onlyOverridenMethodsOf(scopeSettings);
            if (overrideMatcher != null) {
                builder.and(overrideMatcher);
            }
        }

        if (builder.isEmpty()) {
            return ElementMatchers.any();
        } else {
            return builder.build();
        }
    }

    /**
     * Processing a single {@link MethodMatcherSettings} and adding it to the given {@link MatcherChainBuilder}.
     */
    private void processMethod(MatcherChainBuilder<MethodDescription> builder, MethodMatcherSettings matcherSettings) {
        MatcherChainBuilder<MethodDescription> innerBuilder = new MatcherChainBuilder<>();

        innerBuilder.and(matcherSettings.getIsConstructor(), isConstructor());
        innerBuilder.and(SpecialElementMatchers.visibilityIs(matcherSettings.getVisibility()));
        innerBuilder.and(SpecialElementMatchers.argumentsAre(matcherSettings.getArguments()));
        innerBuilder.and(SpecialElementMatchers.annotatedWith(matcherSettings.getAnnotations()));

        if (!matcherSettings.getIsConstructor()) {
            innerBuilder.and(SpecialElementMatchers.nameIs(matcherSettings));

            if (matcherSettings.getIsSynchronized() != null) {
                innerBuilder.and(matcherSettings.getIsSynchronized(), isSynchronized());
            }
        }

        builder.or(innerBuilder.build());
    }
}
