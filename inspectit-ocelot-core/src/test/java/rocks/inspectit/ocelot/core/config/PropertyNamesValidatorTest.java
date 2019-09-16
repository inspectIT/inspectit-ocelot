package rocks.inspectit.ocelot.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class PropertyNamesValidatorTest {

    PropertyNamesValidator validator;

    @BeforeEach
    void buildValidator() {
        validator = new PropertyNamesValidator();
    }

    @Nested
    public class CheckPropertyName {
        @Test
        void wrongProperty() {
            String property = "inspectit.iDoNotExist";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(false);
        }

        @Test
        void correctPropertyButNotAtPathEnd() {
            String property = "inspectit.service-name";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(true);
        }

        @Test
        void emptyString() {
            String property = "";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(false);
        }

        @Test
        void noneInspectitInput() {
            String property = "thisHasNothingToDoWithInspectit";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(false);
        }

        @Test
        void endsInWildcardType() {
            String property = "inspectit.instrumentation.actions.string_replace_all.input.regex";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(true);
        }
    }
}