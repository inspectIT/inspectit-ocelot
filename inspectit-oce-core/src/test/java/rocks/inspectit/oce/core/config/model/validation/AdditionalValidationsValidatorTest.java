package rocks.inspectit.oce.core.config.model.validation;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            MutableBoolean anyCalled = new MutableBoolean(false);
            new AdditionalValidationsValidator().isValid(new Object() {
                @AdditionalValidation
                public void dontCallMe() {
                    anyCalled.setTrue();
                }

                @AdditionalValidation
                public void dontCallMeNeither(ViolationBuilder builder, String somethingelse) {
                    anyCalled.setTrue();
                }
            }, ctx);

            assertThat(anyCalled.getValue()).isFalse();
        }

        @Test
        void testMethodsWithCorrectSignaturesInvoked() {
            MutableBoolean anyCalled = new MutableBoolean(false);
            new AdditionalValidationsValidator().isValid(new Object() {

                @AdditionalValidation
                public void plsCallMe(ViolationBuilder builder) {
                    anyCalled.setTrue();
                }
            }, ctx);

            assertThat(anyCalled.getValue()).isTrue();
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
