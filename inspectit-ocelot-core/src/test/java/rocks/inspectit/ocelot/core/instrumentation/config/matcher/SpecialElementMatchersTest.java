package rocks.inspectit.ocelot.core.instrumentation.config.matcher;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MethodMatcherSettings.AccessModifier;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.NameMatcherSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SpecialElementMatchersTest {

    @Nested
    public class NameIs {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(null);

            assertThat(result).isNull();
        }

        @Test
        public void validSettings() {
            NameMatcherSettings settings = new NameMatcherSettings();
            settings.setName("name");
            settings.setMatcherMode(MatcherMode.MATCHES);

            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(settings);

            assertThat(result).isEqualTo(nameMatches("name"));
        }

        @Test
        public void emptyName() {
            NameMatcherSettings settings = new NameMatcherSettings();
            settings.setMatcherMode(MatcherMode.MATCHES);

            ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(settings);

            assertThat(result).isNull();
        }

        @Test
        public void verifyAllMatcherModesHandled() {
            for (MatcherMode mode : MatcherMode.values()) {
                NameMatcherSettings settings = new NameMatcherSettings();
                settings.setName("something");
                ElementMatcher.Junction<NamedElement> result = SpecialElementMatchers.nameIs(settings);
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    public class NotEquals {

        // define some name pattern
        String NAME_PATTERN_A = "rocks.inspectit.ocelot.core.class.method";

        String NAME_PATTERN_A_UPPERCASE = NAME_PATTERN_A.toUpperCase();

        String NAME_PATTERN_B = "rocks.inspectit.ocelot.core.class.other";

        String NAME_PATTERN_C = NAME_PATTERN_A + "2";

        @Test
        public void notEquals() {
            // create the NameMatcherSettings
            NameMatcherSettings notEqualsSettings = new NameMatcherSettings();
            notEqualsSettings.setName(NAME_PATTERN_A);
            notEqualsSettings.setMatcherMode(MatcherMode.NOT_EQUALS_FULLY);
            // get the ElementMatcher
            ElementMatcher.Junction<NamedElement> matcherNotEquals = SpecialElementMatchers.nameIs(notEqualsSettings);

            // make sure that only the original namePattern does not match
            assertThat(matcherNotEquals.matches(() -> NAME_PATTERN_A)).isFalse();

            // every other string must match
            assertThat(matcherNotEquals.matches(() -> NAME_PATTERN_A_UPPERCASE)).isTrue();
            assertThat(matcherNotEquals.matches(() -> NAME_PATTERN_B)).isTrue();
            assertThat(matcherNotEquals.matches(() -> NAME_PATTERN_C)).isTrue();
        }

        @Test
        public void notEqualsIgnoreCase() {

            // create the NameMatcherSettings
            NameMatcherSettings notEqualsIgnoreCaseSettings = new NameMatcherSettings();
            notEqualsIgnoreCaseSettings.setName(NAME_PATTERN_A);
            notEqualsIgnoreCaseSettings.setMatcherMode(MatcherMode.NOT_EQUALS_FULLY_IGNORE_CASE);

            // get the ElementMatcher
            ElementMatcher.Junction<NamedElement> matcherNotEqualsIgnoreCase = SpecialElementMatchers.nameIs(notEqualsIgnoreCaseSettings);

            // namePattern and all case-variants may not match
            assertThat(matcherNotEqualsIgnoreCase.matches(() -> NAME_PATTERN_A)).isFalse();
            assertThat(matcherNotEqualsIgnoreCase.matches(() -> NAME_PATTERN_A_UPPERCASE)).isFalse();

            assertThat(matcherNotEqualsIgnoreCase.matches(() -> NAME_PATTERN_B)).isTrue();
            assertThat(matcherNotEqualsIgnoreCase.matches(() -> NAME_PATTERN_C)).isTrue();
        }
    }

    @Nested
    public class ArgumentsAre {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptyArguments() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(Collections.emptyList());

            assertThat(result).isEqualTo(takesArguments(0));
        }

        @Test
        public void singleArgument() {
            List<String> arguments = Arrays.asList("class0");

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(arguments);

            Object expectedResult = takesArguments(1).and(takesArgument(0, named("class0")));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void multipleArguments() {
            List<String> arguments = Arrays.asList("class0", "class1");

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.argumentsAre(arguments);

            Object expectedResult = takesArguments(2).and(takesArgument(0, named("class0")))
                    .and(takesArgument(1, named("class1")));
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    public class VisibilityIs {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptyModifier() {
            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(Collections.emptyList());

            assertThat(result).isNull();
        }

        @Test
        public void onlyPublic() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PUBLIC);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPublic();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyPrivate() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PRIVATE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPrivate();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyProtected() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PROTECTED);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isProtected();
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyPackage() {
            List<AccessModifier> modifiers = Collections.singletonList(AccessModifier.PACKAGE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = not(isPublic().or(isProtected()).or(isPrivate()));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void multipleModifiers() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PRIVATE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            Object expectedResult = isPublic().or(isPrivate());
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void eachModifier() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PRIVATE, AccessModifier.PROTECTED, AccessModifier.PACKAGE);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.visibilityIs(modifiers);

            assertThat(result).isNull();
        }

        @Test
        public void duplicateModifiers() {
            List<AccessModifier> modifiers = Arrays.asList(AccessModifier.PUBLIC, AccessModifier.PUBLIC);

            Object result = SpecialElementMatchers.visibilityIs(modifiers);

            assertThat(result).isEqualTo(isPublic());
        }
    }

    @Nested
    public class OnlyOverridenMethodsOf {

        @Test
        public void nullInterfacesAndSuperclass() {
            InstrumentationScopeSettings scope = new InstrumentationScopeSettings();

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            assertThat(result).isNull();
        }

        @Test
        public void onlyInterface() {
            ElementDescriptionMatcherSettings interfaceSettings = new ElementDescriptionMatcherSettings();
            interfaceSettings.setName("interface1");

            InstrumentationScopeSettings scope = new InstrumentationScopeSettings();
            scope.setInterfaces(Collections.singletonList(interfaceSettings));

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("interface1").and(isInterface()));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlySuperclass() {
            ElementDescriptionMatcherSettings superclassSettings = new ElementDescriptionMatcherSettings();
            superclassSettings.setName("superclass1");

            InstrumentationScopeSettings scope = new InstrumentationScopeSettings();
            scope.setSuperclass(superclassSettings);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("superclass1").and(not(isInterface())));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void fullScope() {
            ElementDescriptionMatcherSettings interfaceSettings = new ElementDescriptionMatcherSettings();
            interfaceSettings.setName("interface1");
            ElementDescriptionMatcherSettings superclassSettings = new ElementDescriptionMatcherSettings();
            superclassSettings.setName("superclass1");

            InstrumentationScopeSettings scope = new InstrumentationScopeSettings();
            scope.setInterfaces(Collections.singletonList(interfaceSettings));
            scope.setSuperclass(superclassSettings);

            ElementMatcher.Junction<MethodDescription> result = SpecialElementMatchers.onlyOverridenMethodsOf(scope);

            Object expectedResult = isOverriddenFrom(named("interface1").and(isInterface())
                    .or(named("superclass1").and(not(isInterface()))));
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    public class DescribedBy {

        @Test
        public void nullArgument() {
            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(null);

            assertThat(result).isNull();
        }

        @Test
        public void emptySettings() {
            ElementDescriptionMatcherSettings settings = new ElementDescriptionMatcherSettings();

            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(settings);

            assertThat(result).isNull();
        }

        @Test
        public void onlyName() {
            ElementDescriptionMatcherSettings settings = new ElementDescriptionMatcherSettings();
            settings.setName("name1");

            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(settings);

            Object expectedResult = named("name1");
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void onlyAnnotation() {
            NameMatcherSettings annotationMatcher = new NameMatcherSettings();
            annotationMatcher.setName("annotation1");
            ElementDescriptionMatcherSettings settings = new ElementDescriptionMatcherSettings();
            settings.setAnnotations(Collections.singletonList(annotationMatcher));

            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(settings);

            Object expectedResult = IsAnnotatedMatcher.of(named("annotation1"));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void nameAndAnnotation() {
            NameMatcherSettings annotationMatcher = new NameMatcherSettings();
            annotationMatcher.setName("annotation1");
            ElementDescriptionMatcherSettings settings = new ElementDescriptionMatcherSettings();
            settings.setName("name1");
            settings.setAnnotations(Collections.singletonList(annotationMatcher));

            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(settings);

            Object expectedResult = named("name1").and(IsAnnotatedMatcher.of(named("annotation1")));
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void multipleAnnotations() {
            NameMatcherSettings annotationMatcher1 = new NameMatcherSettings();
            annotationMatcher1.setName("annotation1");
            NameMatcherSettings annotationMatcher2 = new NameMatcherSettings();
            annotationMatcher2.setName("annotation2");
            ElementDescriptionMatcherSettings settings = new ElementDescriptionMatcherSettings();
            settings.setAnnotations(Arrays.asList(annotationMatcher1, annotationMatcher2));

            ElementMatcher.Junction<TypeDescription> result = SpecialElementMatchers.describedBy(settings);

            Object expectedResult = IsAnnotatedMatcher.of(named("annotation1"))
                    .and(IsAnnotatedMatcher.of(named("annotation2")));
            assertThat(result).isEqualTo(expectedResult);
        }

    }
}