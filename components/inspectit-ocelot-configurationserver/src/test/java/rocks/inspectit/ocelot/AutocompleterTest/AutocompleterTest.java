package rocks.inspectit.ocelot.AutocompleterTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.autocomplete.Autocompleter;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


public class AutocompleterTest {

    Autocompleter completer;

    @BeforeEach
    void buildValidator() {
        completer = new Autocompleter();
    }

    @Nested
    public class Parse {
        @Test
        void kebabCaseTest() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome"));
            assertThat(completer.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(completer.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first", "second"));
            assertThat(completer.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first.second"));
            assertThat(completer.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                completer.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("invalid property path");
            }
        }
    }

    @Nested
    public class CleanString {

        @Test
        void cleanTest() {
            assertThat(completer.cleanString("[some.literal]")).isEqualTo("some.literal");
        }
    }

    @Nested
    public class ToCamelCase {

        @Test
        void toCamelCaseTest() {
            assertThat(completer.toCamelCase("i-want-to-be-camel-case")).isEqualTo("iWantToBeCamelCase");
        }
    }

    @Nested
    public class toKebabCase {

        @Test
        void toKebabCase() {
            assertThat(completer.toKebabCase("makeMeAKebab")).isEqualTo("make-me-a-kebab");
        }
    }

    @Nested
    public class CheckPropertyName {

        @Test
        void propertyNameValidTest() {
            assertThat(completer.checkPropertyName("inspectit.self-monitoring.")).isEqualTo(true);

        }

        @Test
        void propertyNameInvalidTest() {
            assertThat(completer.checkPropertyName("insptit.self-monitoring.")).isEqualTo(false);
        }
    }

    @Nested
    public class CheckPropertyExists {

    }
}
