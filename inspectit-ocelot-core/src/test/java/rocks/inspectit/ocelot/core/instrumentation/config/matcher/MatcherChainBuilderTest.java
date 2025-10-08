package rocks.inspectit.ocelot.core.instrumentation.config.matcher;

import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class MatcherChainBuilderTest {

    MatcherChainBuilder<Object> builder;

    ElementMatcher.Junction<Object> anyMatcher = ElementMatchers.any();

    @BeforeEach
    void beforeEach() {
        builder = new MatcherChainBuilder<>();
    }

    @Nested
    class Or {

        @Test
        void nullMatcher() {
            builder.or(null);

            assertThat(builder.build()).isNull();
            assertThat(builder.isEmpty()).isTrue();
        }

        @Test
        void singleMatcher() {
            builder.or(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
            assertThat(builder.isEmpty()).isFalse();
        }

        @Test
        void multipleMatcher() {
            builder.or(anyMatcher);
            builder.or(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.or(anyMatcher));
        }

        @Test
        void nullOnNotEmpty() {
            builder.or(anyMatcher);
            builder.or(null);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }
    }

    @Nested
    class And {

        @Test
        void nullMatcher() {
            builder.and(null);

            assertThat(builder.build()).isNull();
            assertThat(builder.isEmpty()).isTrue();
        }

        @Test
        void singleMatcher() {
            builder.and(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
            assertThat(builder.isEmpty()).isFalse();
        }

        @Test
        void multipleMatcher() {
            builder.and(anyMatcher);
            builder.and(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.and(anyMatcher));
        }

        @Test
        void nullOnNotEmpty() {
            builder.and(anyMatcher);
            builder.and(null);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }

        @Test
        void conditionalTrue() {
            builder.and(true, anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }

        @Test
        void conditionalFalse() {
            builder.and(false, anyMatcher);

            assertThat(builder.build()).isEqualTo(not(anyMatcher));
        }

        @Test
        void conditionalFalseOnNotEmppty() {
            builder.and(anyMatcher);
            builder.and(false, anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.and(not(anyMatcher)));
        }
    }

    @Nested
    class IsEmpty {

        @Test
        void empty() {
            boolean result = builder.isEmpty();

            assertThat(result).isTrue();
        }

        @Test
        void notEmpty() {
            builder.and(ElementMatchers.any());

            boolean result = builder.isEmpty();

            assertThat(result).isFalse();
        }
    }
}
