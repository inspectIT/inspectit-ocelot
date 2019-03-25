package rocks.inspectit.ocelot.core.instrumentation.config.model;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InstrumentationScopeTest {

    @Nested
    public class Equals {

        @Test
        public void isEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.equals(scopeB)).isTrue();
        }

        @Test
        public void methodNotEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass_thisIsMissing")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.equals(scopeB)).isFalse();
        }

        @Test
        public void classNotEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever_thisIsMissing")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.equals(scopeB)).isFalse();
        }
    }

    @Nested
    public class HashCode {

        @Test
        public void isEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.hashCode()).isEqualTo(scopeB.hashCode());
        }

        @Test
        public void methodNotEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass_thisIsMissing")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.hashCode()).isNotEqualTo(scopeB.hashCode());
        }

        @Test
        public void classNotEqual() {

            ElementMatcher.Junction<TypeDescription> typeA = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever_thisIsMissing")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodA = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));


            ElementMatcher.Junction<TypeDescription> typeB = ElementMatchers.named("name")
                    .and(ElementMatchers.hasSuperType(ElementMatchers.nameMatches("whatever")))
                    .and(ElementMatchers.isInterface());

            ElementMatcher.Junction<MethodDescription> methodB = ElementMatchers.named("name")
                    .and(ElementMatchers.isOverriddenFrom(ElementMatchers.nameContains("someClass")));

            InstrumentationScope scopeA = new InstrumentationScope(typeA, methodA);
            InstrumentationScope scopeB = new InstrumentationScope(typeB, methodB);

            assertThat(scopeA.hashCode()).isNotEqualTo(scopeB.hashCode());
        }
    }
}