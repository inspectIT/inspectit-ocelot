package rocks.inspectit.oce.core.instrumentation.config.matcher;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.NameMatcher;
import net.bytebuddy.matcher.StringMatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.DescriptionMatcherSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.scope.NameMatcherSettings;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static rocks.inspectit.oce.core.config.model.instrumentation.scope.MethodMatcherSettings.AccessModifier;

/**
 * This class provides advanced and custom ElementMatchers.
 */
public class SpecialElementMatchers {

    private SpecialElementMatchers() {
    }

    /**
     * Creates an {@link ElementMatcher} matching items with the given name settings.
     */
    public static <T extends NamedElement> ElementMatcher.Junction<T> nameIs(NameMatcherSettings settings) {
        if (settings == null) {
            return null;
        }

        String namePattern = settings.getName();

        if (StringUtils.isNotEmpty(namePattern)) {
            StringMatcher.Mode matcherMode = settings.getMatcherMode();
            return new NameMatcher<>(new StringMatcher(namePattern, matcherMode));
        } else {
            return null;
        }
    }


    public static ElementMatcher.Junction<TypeDescription> describedBy(DescriptionMatcherSettings settings) {
        MatcherChainBuilder<TypeDescription> builder = new MatcherChainBuilder<>();

        builder.and(nameIs(settings));
        builder.and(annotatedWith(settings.getAnnotation()));

        return builder.build();
    }

    /**
     * Creating an {@link ElementMatcher} matching methods which takes arguments equals to the given array. The array consists
     * of String representing full qualified class names.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> argumentsAre(List<String> arguments) {
        if (arguments == null) {
            return null;
        } else if (arguments.size() == 0) {
            return takesArguments(0);
        } else {
            MatcherChainBuilder<T> builder = new MatcherChainBuilder<>();
            builder.and(takesArguments(arguments.size()));

            for (int i = 0; i < arguments.size(); i++) {
                builder.and(takesArgument(i, named(arguments.get(i))));
            }

            return builder.build();
        }
    }

    /**
     * Creating an {@link ElementMatcher} matching methods with the given access modifiers.
     * Returns null in case the given array contains each modifier element at least once.
     */
    public static <T extends ModifierReviewable.OfByteCodeElement> ElementMatcher.Junction<T> visibilityIs(List<AccessModifier> accessModifiers) {
        if (CollectionUtils.isEmpty(accessModifiers)) {
            return null;
        }

        boolean isPublic = accessModifiers.contains(AccessModifier.PUBLIC);
        boolean isProtected = accessModifiers.contains(AccessModifier.PROTECTED);
        boolean isPackage = accessModifiers.contains(AccessModifier.PACKAGE);
        boolean isPrivate = accessModifiers.contains(AccessModifier.PRIVATE);
        if (isPublic && isProtected && isPackage && isPrivate) {
            return null;
        }

        MatcherChainBuilder<T> builder = new MatcherChainBuilder<>();

        if (isPublic) {
            builder.or(isPublic());
        }
        if (isProtected) {
            builder.or(isProtected());
        }
        if (isPackage) {
            builder.or(isPackagePrivate());
        }
        if (isPrivate) {
            builder.or(isPrivate());
        }

        return builder.build();
    }

    /**
     * Matches any virtual method with a signature that is compatible to a method that is declared by a type that matches the supplied type scope.
     */
    public static <T extends MethodDescription> ElementMatcher.Junction<T> onlyOverridenMethodsOf(InstrumentationScopeSettings scopeSettings) {
        MatcherChainBuilder<TypeDescription> builder = new MatcherChainBuilder<>();

        if (scopeSettings.getInterfaces() != null) {
            scopeSettings.getInterfaces().forEach(i -> {
                ElementMatcher.Junction<NamedElement> interfaceMatcher = nameIs(i);
                if (interfaceMatcher != null) {
                    builder.or(interfaceMatcher.and(isInterface()));
                }
            });
        }

        if (scopeSettings.getSuperclass() != null) {
            ElementMatcher.Junction<NamedElement> superclassMatcher = nameIs(scopeSettings.getSuperclass());
            if (superclassMatcher != null) {
                builder.or(superclassMatcher.and(not(isInterface())));
            }
        }

        if (builder.isEmpty()) {
            return null;
        } else {
            return isOverriddenFrom(builder.build());
        }
    }

    public static <T extends AnnotationSource> ElementMatcher.Junction<T> annotatedWith(NameMatcherSettings matcherSettings) {
        ElementMatcher.Junction<NamedElement> nameMatcher = nameIs(matcherSettings);
        if (nameMatcher != null) {
            return IsAnnotatedMatcher.of(nameMatcher);
        }
        return null;
    }
}
