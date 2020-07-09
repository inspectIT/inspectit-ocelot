package rocks.inspectit.ocelot.config.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyPathHelperTest {

    @Nested
    public class Parse {
        @Test
        void kebabCaseTest() {
            List<String> output = Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome");

            assertThat(PropertyPathHelper.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(PropertyPathHelper.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(PropertyPathHelper.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            List<String> output = Arrays.asList("inspectit", "property", "first", "second");

            assertThat(PropertyPathHelper.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            List<String> output = Arrays.asList("inspectit", "property", "first.second");

            assertThat(PropertyPathHelper.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                PropertyPathHelper.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("invalid property path");
            }
        }

    }

    @Nested
    public class IsBean {

        @Test
        public void genericTypeIsNotABean() throws NoSuchFieldException {
            Type type = new Object() {
                AtomicReference<Boolean> genericMember;
            }.getClass().getDeclaredField("genericMember").getGenericType();

            boolean result = PropertyPathHelper.isBean(type);
            assertThat(result).isFalse();
        }

        @Test
        public void mapIsNotABean() {
            boolean result = PropertyPathHelper.isBean(Map.class);
            assertThat(result).isFalse();
        }

        @Test
        public void setIsNotABean() {
            boolean result = PropertyPathHelper.isBean(Set.class);
            assertThat(result).isFalse();
        }

        @Test
        public void listIsNotABean() {
            boolean result = PropertyPathHelper.isBean(List.class);
            assertThat(result).isFalse();
        }

        @Test
        public void durationIsNotABean() {
            boolean result = PropertyPathHelper.isBean(Duration.class);
            assertThat(result).isFalse();
        }

        @Test
        public void integerIsNotABean() {
            boolean result = PropertyPathHelper.isBean(Integer.class);
            assertThat(result).isFalse();
        }

        @Test
        public void pojoIsABean() {
            Class<?> pojo = new Object() {
                String member;
            }.getClass();

            boolean result = PropertyPathHelper.isBean(pojo);
            assertThat(result).isTrue();
        }
    }

    @Nested
    public class IsTerminal {

        @Test
        public void terminalType() {
            boolean result = PropertyPathHelper.isTerminal(Boolean.class);

            assertThat(result).isTrue();
        }

        @Test
        public void primitive() {
            boolean result = PropertyPathHelper.isTerminal(boolean.class);

            assertThat(result).isTrue();
        }

        @Test
        public void enumType() {
            boolean result = PropertyPathHelper.isTerminal(DayOfWeek.class);

            assertThat(result).isTrue();
        }

        @Test
        public void nonTerminalType() {
            boolean result = PropertyPathHelper.isTerminal(getClass());

            assertThat(result).isFalse();
        }

    }

    @Nested
    public class CheckPropertyExists {
        @Test
        void terminalTest() {
            List<String> list = Arrays.asList("config", "file-based", "path");
            Type output = String.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void nonTerminalTest() {
            List<String> list = Arrays.asList("exporters", "metrics", "prometheus");
            Type output = PrometheusExporterSettings.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void emptyString() {
            List<String> list = Arrays.asList("");
            Type output = null;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void existingList() {
            List<String> list = Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode");
            Type output = MatcherMode.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void existingMap() {
            List<String> list = Arrays.asList("metrics", "definitions", "jvm/gc/concurrent/phase/time", "description");
            Type output = String.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void readMethodIsNull() {
            List<String> list = Arrays.asList("instrumentation", "data", "method_duration", "is-tag");
            Type output = Boolean.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void endsInWildcardType() {
            List<String> list = Arrays.asList("instrumentation", "actions", "string_replace_all", "input", "regex");
            Type output = String.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }
    }

    @Nested
    public class CheckPropertyExistsInMap {
        @Test
        void nonTerminalMapTest() {
            List<String> list = Arrays.asList("matcher-mode");

            assertThat(PropertyPathHelper.getPathEndType(list, Map.class)).isEqualTo(null);

        }
    }

    @Nested
    public class CheckPropertyExistsInList {
        @Test
        void nonTerminalListTest() {
            List<String> list = Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode");
            Type output = MatcherMode.class;

            assertThat(PropertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }
    }

    @Nested
    public class ComparePaths {
        @Test
        void identicalPaths() {
            List<String> pathA = Arrays.asList("i", "am", "a", "path");
            List<String> pathB = Arrays.asList("i", "am", "a", "path");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(true);
        }

        @Test
        void identicalPathsWildcard() {
            List<String> pathA = Arrays.asList("i", "am", "*", "path");
            List<String> pathB = Arrays.asList("i", "am", "a", "path");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(true);
        }

        @Test
        void doubleWildcard() {
            List<String> pathA = Arrays.asList("i", "am", "*", "path");
            List<String> pathB = Arrays.asList("i", "am", "*", "path");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(true);
        }

        @Test
        void doubleWildcardDifferentIndex() {
            List<String> pathA = Arrays.asList("i", "am", "*", "path");
            List<String> pathB = Arrays.asList("i", "*", "a", "path");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(true);
        }

        @Test
        void nonIdenticalPaths() {
            List<String> pathA = Arrays.asList("i", "am", "a", "path");
            List<String> pathB = Arrays.asList("me", "too");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(false);
        }

        @Test
        void nonIdenticalPathsSameLength() {
            List<String> pathA = Arrays.asList("i", "am", "a", "path");
            List<String> pathB = Arrays.asList("i", "am", "another", "path");

            assertThat(PropertyPathHelper.comparePaths(pathA, pathB)).isEqualTo(false);
        }
    }

    @Nested
    public class HasPathPrefix {
        @Test
        void identicalPrefix() {
            List<String> path = Arrays.asList("i", "am", "a", "path", "and", "that's", "cool");
            List<String> prefix = Arrays.asList("i", "am", "a", "path");

            assertThat(PropertyPathHelper.hasPathPrefix(path, prefix)).isEqualTo(true);
        }

        @Test
        void identicalPrefixWildcard() {
            List<String> path = Arrays.asList("i", "*", "a", "path", "and", "that's", "cool");
            List<String> prefix = Arrays.asList("i", "am", "a", "path");

            assertThat(PropertyPathHelper.hasPathPrefix(path, prefix)).isEqualTo(true);
        }

        @Test
        void nonIdenticalPath() {
            List<String> path = Arrays.asList("i", "am", "a", "path");
            List<String> prefix = Arrays.asList("i", "am", "too");

            assertThat(PropertyPathHelper.hasPathPrefix(path, prefix)).isEqualTo(false);
        }
    }
}

