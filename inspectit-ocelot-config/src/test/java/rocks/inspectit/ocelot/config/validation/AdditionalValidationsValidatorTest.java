package rocks.inspectit.ocelot.config.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdditionalValidationsValidatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HibernateConstraintValidatorContext ctx;

    @Nested
    class IsValid {

        @BeforeEach
        void initMock() {
            doReturn(ctx).when(ctx).unwrap(any());
        }

        @Test
        void testMethodsWithIncorrectSignaturesNotInvoked() {
            AtomicBoolean anyCalled = new AtomicBoolean(false);
            new AdditionalValidationsValidator().isValid(new Object() {
                @AdditionalValidation
                public void dontCallMe() {
                    anyCalled.set(true);
                }

                @AdditionalValidation
                public void dontCallMeNeither(ViolationBuilder builder, String somethingelse) {
                    anyCalled.set(true);
                }
            }, ctx);

            assertThat(anyCalled.get()).isFalse();
        }

        @Test
        void testMethodsWithCorrectSignaturesInvoked() {
            AtomicBoolean anyCalled = new AtomicBoolean(false);
            new AdditionalValidationsValidator().isValid(new Object() {

                @AdditionalValidation
                public void plsCallMe(ViolationBuilder builder) {
                    anyCalled.set(true);
                }
            }, ctx);

            assertThat(anyCalled.get()).isTrue();
        }

        @Test
        void testViolationsPublished() {
            new AdditionalValidationsValidator().isValid(new Object() {

                @AdditionalValidation
                public void plsCallMe(ViolationBuilder builder) {
                    builder.message("my violation").buildAndPublish();
                }
            }, ctx);

            verify(ctx, times(1)).buildConstraintViolationWithTemplate(eq("my violation"));
        }

    }
}
