package rocks.inspectit.ocelot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class YamlValidatorTest {

    YamlValidator validator;

    @BeforeEach
    void buildValidator() {
        validator = new YamlValidator();
    }

    @Nested
    public class Parse {

        @Test
        void kebabCaseTest() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome"));
            assertThat(validator.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(validator.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();
            assertThat(validator.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first", "second"));
            assertThat(validator.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first.second"));
            assertThat(validator.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                validator.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage().equals("invalid property path")).isEqualTo(true);
            }
        }

    }

    @Nested
    public class CheckPropertyName {
        @Test
        void wrongProperty() {
            String property = "inspectit.iDoNotExist";
            assertThat(validator.checkPropertyName(property)).isEqualTo(true);
        }

        @Test
        void correctProperty() {
            String property = "inspectit.service-name";
            assertThat(validator.checkPropertyName(property)).isEqualTo(false);
        }

        @Test
        void emptyString() {
            String property = "";
            assertThat(validator.checkPropertyName(property)).isEqualTo(false);
        }

        @Test
        void noneInspectitInput() {
            String property = "thisHasNothingToDoWithInspectit";
            assertThat(validator.checkPropertyName(property)).isEqualTo(false);
        }
    }

    @Nested
    public class CheckPropertyExists {

        @Test
        void workingTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("exporters", "metrics", "prometheus"));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void emptyString() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(""));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(false);
        }

        @Test
        void existingList() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode"));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void existingMap() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("metrics", "definitions", "jvm/gc/concurrent/phase/time", "description"));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void readMethodIsNull() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "data", "method_duration", "is-tag"));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void endsInWildcardType() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex"));
            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }
    }

    @Nested
    public class CheckPropertyExistsInMap {

        @Test
        void mapTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("matcher-mode"));
            assertThat(validator.checkPropertyExistsInMap(list, Map.class)).isEqualTo(true);

        }
    }


    @Nested
    public class CheckPropertyExistsInList {

        @Test
        void listTest() {
            ArrayList<String> list = new ArrayList(Arrays.asList("matcher-mode"));
            assertThat(validator.checkPropertyExistsInList(list, List.class)).isEqualTo(true);
        }
    }
}