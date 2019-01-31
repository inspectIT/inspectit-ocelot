package rocks.inspectit.oce.core.instrumentation.config;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.NameMatcher;
import net.bytebuddy.matcher.StringMatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.NameMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.TypeScope;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings.AccessModifier;

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

        if (source != null && source.getScopes() != null) {
            for (Map.Entry<String, InstrumentationScopeSettings> scopeEntry : source.getScopes().entrySet()) {
                InstrumentationScopeSettings scopeSettings = scopeEntry.getValue();
                if (scopeSettings.getTypeScope() == null) {
                    log.warn("Processing of scope '{}' skipped. Empty type-scope is not allowed!", scopeEntry.getKey());
                    continue;
                }

                log.debug("Processing scope '{}'.", scopeEntry.getKey());

                InstrumentationScope scope = resolveScope(scopeEntry.getKey(), scopeSettings);

                log.debug("|> Type scope: {}", scope.getTypeMatcher() != null ? scope.getTypeMatcher().toString() : "-");
                log.debug("|> Method scope: {}", scope.getMethodMatcher() != null ? scope.getMethodMatcher().toString() : "-");

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
    private InstrumentationScope resolveScope(String key, InstrumentationScopeSettings scopeSettings) {
        ElementMatcher.Junction<TypeDescription> typeMatcher = buildTypeMatcher(scopeSettings);
        ElementMatcher.Junction<MethodDescription> methodMatcher = buildMethodMatcher(scopeSettings, typeMatcher);

        if (typeMatcher == null) {
            typeMatcher = any();
            log.warn("|> The defined scope '{}' is not narrowing the type-scope, thus, matching ANY types! This may cause a negative performance impact!", key);
        }

        return new InstrumentationScope(typeMatcher, methodMatcher);
    }

    /**
     * Creating an {@link ElementMatcher} which matches the specified methods.
     */
    private ElementMatcher.Junction<MethodDescription> buildMethodMatcher(InstrumentationScopeSettings scopeSettings, ElementMatcher.Junction<TypeDescription> typeMatcher) {
        MatcherBuilder<MethodDescription> builder = new MatcherBuilder<>();

        if (scopeSettings.getMethodScope() != null) {
            scopeSettings.getMethodScope().forEach(m -> processMethod(builder, m));
        }

        if (builder.isEmpty()) {
            builder.and(any());
        }

        ElementMatcher.Junction<MethodDescription> matcher = builder.build();

        if (scopeSettings.getAdvanced() != null && scopeSettings.getAdvanced().getInstrumentOnlyInheritedMethods()) {
            MatcherBuilder<TypeDescription> superBuilder = new MatcherBuilder<>();

            if (scopeSettings.getTypeScope().getInterfaces() != null) {
                scopeSettings.getTypeScope()
                        .getInterfaces()
                        .forEach(i -> superBuilder.or(buildNameMatcher(i)));
            }
            if (scopeSettings.getTypeScope().getSuperclass() != null) {
                superBuilder.or(buildNameMatcher(scopeSettings.getTypeScope().getSuperclass()));
            }

            if (!superBuilder.isEmpty()) {
                matcher = isOverriddenFrom(superBuilder.build()).and(builder.build());
            }
        }

        return matcher;
    }

    /**
     * Processing a single {@link MethodMatcherSettings} and adding it to the given {@link MatcherBuilder}.
     */
    private void processMethod(MatcherBuilder<MethodDescription> builder, MethodMatcherSettings matcherSettings) {
        MatcherBuilder<MethodDescription> innerBuilder = new MatcherBuilder<>();

        innerBuilder.and(matcherSettings.getIsConstructor(), isConstructor());
        innerBuilder.and(buildVisibilityMatcher(matcherSettings.getVisibility()));
        innerBuilder.and(buildArgumentMatcher(matcherSettings.getArguments()));

        if (!matcherSettings.getIsConstructor()) {
            ElementMatcher.Junction<MethodDescription> nameMatcher = buildNameMatcher(matcherSettings);
            innerBuilder.and(nameMatcher);
            if (matcherSettings.getIsSynchronized() != null) {
                innerBuilder.and(matcherSettings.getIsSynchronized(), isSynchronized());
            }
        }

        builder.or(innerBuilder.build());
    }

    /**
     * Creating an {@link ElementMatcher} matching methods which takes arguments equals to the given array. The array consists
     * of String representing full qualified class names.
     */
    private ElementMatcher.Junction<MethodDescription> buildArgumentMatcher(String[] arguments) {
        if (arguments == null) {
            return null;
        } else if (arguments.length == 0) {
            return takesArguments(0);
        } else {
            MatcherBuilder<MethodDescription> builder = new MatcherBuilder<>();

            for (int i = 0; i < arguments.length; i++) {
                builder.and(takesArgument(i, named(arguments[i])));
            }

            return builder.build();
        }
    }

    /**
     * Creating an {@link ElementMatcher} matching methods with the given access modifiers.
     */
    private ElementMatcher.Junction<MethodDescription> buildVisibilityMatcher(AccessModifier[] modifiers) {
        if (modifiers == null) {
            return null;
        }

        List<AccessModifier> accessModifiers = Arrays.asList(modifiers);
        boolean isPublic = accessModifiers.contains(AccessModifier.PUBLIC);
        boolean isProtected = accessModifiers.contains(AccessModifier.PROTECTED);
        boolean isPackage = accessModifiers.contains(AccessModifier.PACKAGE);
        boolean isPrivate = accessModifiers.contains(AccessModifier.PRIVATE);
        if (isPublic && isProtected && isPackage && isPrivate) {
            return null;
        }

        MatcherBuilder<MethodDescription> builder = new MatcherBuilder<>();

        for (AccessModifier modifier : modifiers) {
            switch (modifier) {
                case PUBLIC:
                    builder.or(isPublic());
                    break;
                case PROTECTED:
                    builder.or(isProtected());
                    break;
                case PACKAGE:
                    builder.or(isPackagePrivate());
                    break;
                case PRIVATE:
                    builder.or(isPrivate());
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Creates an {@link ElementMatcher} matching items with the given name settings.
     */
    private <N extends NamedElement> ElementMatcher.Junction<N> buildNameMatcher(NameMatcherSettings matcherSettings) {
        String namePattern = matcherSettings.getNamePattern();

        if (StringUtils.isNotEmpty(namePattern)) {
            StringMatcher.Mode matcherMode = matcherSettings.getMatcherMode();
            return new NameMatcher<>(new StringMatcher(namePattern, matcherMode));
        } else {
            return null;
        }
    }

    /**
     * Creates the {@link ElementMatcher} which is matching the scope's type specified in the configuration.
     */
    private ElementMatcher.Junction<TypeDescription> buildTypeMatcher(InstrumentationScopeSettings scopeSettings) {
        if (scopeSettings.getTypeScope() == null) {
            return null;
        }

        MatcherBuilder<TypeDescription> builder = new MatcherBuilder<>();

        if (scopeSettings.getTypeScope().getSuperclass() != null) {
            processSuperType(builder, scopeSettings.getTypeScope().getSuperclass());
        }
        if (scopeSettings.getTypeScope().getInterfaces() != null) {
            scopeSettings.getTypeScope().getInterfaces().forEach(i -> processSuperType(builder, i));
        }
        if (scopeSettings.getTypeScope().getClasses() != null) {
            scopeSettings.getTypeScope().getClasses().forEach(c -> processClass(builder, c));
        }

        if (scopeSettings.getAdvanced() != null && scopeSettings.getAdvanced().getInstrumentOnlyAbstractClasses()) {
            builder.and(isAbstract());
        }

        return builder.build();
    }

    /**
     * Processing the interface and superclass settings of the {@link TypeScope}.
     */
    private void processSuperType(MatcherBuilder<TypeDescription> builder, NameMatcherSettings matcherSettings) {
        ElementMatcher.Junction<TypeDescription> nameMatcher = buildNameMatcher(matcherSettings);
        builder.and(hasSuperType(nameMatcher));
    }

    /**
     * Processing the class settings of the {@link TypeScope}.
     */
    private void processClass(MatcherBuilder<TypeDescription> builder, NameMatcherSettings matcherSettings) {
        ElementMatcher.Junction<TypeDescription> nameMatcher = buildNameMatcher(matcherSettings);
        builder.and(nameMatcher);
    }

}
