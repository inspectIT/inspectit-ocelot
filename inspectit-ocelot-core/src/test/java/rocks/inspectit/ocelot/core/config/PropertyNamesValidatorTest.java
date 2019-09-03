package rocks.inspectit.ocelot.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class PropertyNamesValidatorTest {

    PropertyNamesValidator validator;

    @BeforeEach
    void buildValidator() {
        validator = new PropertyNamesValidator();
    }

    @Nested
    public class Parse {
        @Test
        void kebabCaseTest() {
            List<String> output = Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome");

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
            List<String> output = Arrays.asList("inspectit", "property", "first", "second");

            assertThat(validator.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            List<String> output = Arrays.asList("inspectit", "property", "first.second");

            assertThat(validator.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                validator.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("invalid property path");
            }
        }

    }

    @Nested
    public class IsInvalidPropertyName {
        @Test
        void wrongProperty() {
            String property = "inspectit.iDoNotExist";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(true);
        }

        @Test
        void correctProperty() {
            String property = "inspectit.service-name";

            assertThat(validator.isInvalidPropertyName(property)).isEqualTo(false);
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
    }

    @Nested
    public class CheckPropertyExists {
        @Test
        void termminalTest() {
            List<String> list = Arrays.asList("config", "file-based", "path");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void nonTermminalTest() {
            List<String> list = Arrays.asList("exporters", "metrics", "prometheus");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(false);
        }

        @Test
        void emptyString() {
            List<String> list = Arrays.asList("");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(false);
        }

        @Test
        void existingList() {
            List<String> list = Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void existingMap() {
            List<String> list = Arrays.asList("metrics", "definitions", "jvm/gc/concurrent/phase/time", "description");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void readMethodIsNull() {
            List<String> list = Arrays.asList("instrumentation", "data", "method_duration", "is-tag");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void endsInWildcardType() {
            List<String> list = Arrays.asList("instrumentation", "actions", "string_replace_all", "input", "regex");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(true);
        }

        @Test
        void terminalListTest() {
            List<String> list = Arrays.asList("inspectit", "instrumentation", "scopes", "httpurlconnection_getInputStream.methods", "0", "arguments");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(false);
        }

        @Test
        void nonInspectItInput() {
            List<String> list = Arrays.asList("java", "specification", "version");

            assertThat(validator.checkPropertyExists(list, InspectitConfig.class)).isEqualTo(false);
        }
    }

    @Nested
    public class CheckPropertyExistsInMap {
        @Test
        void nonTerminalMapTest() {
            List<String> list = Arrays.asList("matcher-mode");

            assertThat(validator.checkPropertyExistsInMap(list, Map.class)).isEqualTo(false);

        }
    }

    @Nested
    public class CheckPropertyExistsInList {
        @Test
        void nonTerminalListTest() {
            List<String> list = Arrays.asList("matcher-mode");

            assertThat(validator.checkPropertyExistsInList(list, List.class)).isEqualTo(false);
        }
    }
}