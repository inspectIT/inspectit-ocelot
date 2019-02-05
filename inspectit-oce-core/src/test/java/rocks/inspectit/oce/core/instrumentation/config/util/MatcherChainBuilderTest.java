package rocks.inspectit.oce.core.instrumentation.config.util;

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

    private MatcherChainBuilder<Object> builder;

    private ElementMatcher.Junction<Object> anyMatcher = ElementMatchers.any();

    @BeforeEach
    public void beforeEach() {
        builder = new MatcherChainBuilder<>();
    }

    @Nested
    public class Or {

        @Test
        public void nullMatcher() {
            builder.or(null);

            assertThat(builder.build()).isNull();
            assertThat(builder.isEmpty()).isTrue();
        }

        @Test
        public void singleMatcher() {
            builder.or(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
            assertThat(builder.isEmpty()).isFalse();
        }

        @Test
        public void multipleMatcher() {
            builder.or(anyMatcher);
            builder.or(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.or(anyMatcher));
        }

        @Test
        public void nullOnNotEmpty() {
            builder.or(anyMatcher);
            builder.or(null);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }
    }

    @Nested
    public class And {

        @Test
        public void nullMatcher() {
            builder.and(null);

            assertThat(builder.build()).isNull();
            assertThat(builder.isEmpty()).isTrue();
        }

        @Test
        public void singleMatcher() {
            builder.and(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
            assertThat(builder.isEmpty()).isFalse();
        }

        @Test
        public void multipleMatcher() {
            builder.and(anyMatcher);
            builder.and(anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.and(anyMatcher));
        }

        @Test
        public void nullOnNotEmpty() {
            builder.and(anyMatcher);
            builder.and(null);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }

        @Test
        public void conditionalTrue() {
            builder.and(true, anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher);
        }

        @Test
        public void conditionalFalse() {
            builder.and(false, anyMatcher);

            assertThat(builder.build()).isEqualTo(not(anyMatcher));
        }

        @Test
        public void conditionalFalseOnNotEmppty() {
            builder.and(anyMatcher);
            builder.and(false, anyMatcher);

            assertThat(builder.build()).isEqualTo(anyMatcher.and(not(anyMatcher)));
        }
    }

    @Nested
    public class IsEmpty {

        @Test
        public void empty() {
            boolean result = builder.isEmpty();

            assertThat(result).isTrue();
        }

        @Test
        public void notEmpty() {
            builder.and(ElementMatchers.any());

            boolean result = builder.isEmpty();

            assertThat(result).isFalse();
        }
    }
}