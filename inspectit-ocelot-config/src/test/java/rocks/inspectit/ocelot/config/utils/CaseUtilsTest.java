package rocks.inspectit.ocelot.config.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseUtilsTest {

    @Nested
    public class CamelCaseToKebabCase {

        @Test
        void twoLiteralTest() {
            assertThat(CaseUtils.camelCaseToKebabCase("testName")).isEqualTo("test-name");
        }

        @Test
        void singleLetterLiterals() {
            assertThat(CaseUtils.camelCaseToKebabCase("testNameAB")).isEqualTo("test-name-a-b");
        }

        @Test
        void noCase() {
            assertThat(CaseUtils.camelCaseToKebabCase("test")).isEqualTo("test");
        }

        @Test
        void dividedByFullStop() {
            assertThat(CaseUtils.camelCaseToKebabCase("myPath.exampleClass")).isEqualTo("my-path.example-class");
        }

        @Test
        void nonAlphabeticalBeforeUpperCase() {
            assertThat(CaseUtils.camelCaseToKebabCase("my1Path")).isEqualTo("my1-path");
        }
    }

    @Nested
    public class KebabCaseToCamelCase {

        @Test
        void twoLiteralTest() {
            assertThat(CaseUtils.kebabCaseToCamelCase("test-name")).isEqualTo("testName");
        }

        @Test
        void singleLetterLiterals() {
            assertThat(CaseUtils.kebabCaseToCamelCase("test-name-a-b")).isEqualTo("testNameAB");
        }

        @Test
        void noCase() {
            assertThat(CaseUtils.kebabCaseToCamelCase("test")).isEqualTo("test");
        }

        @Test
        void dividedByFullStop() {
            assertThat(CaseUtils.kebabCaseToCamelCase("my-path.example-class")).isEqualTo("myPath.exampleClass");
        }
    }

    @Nested
    public class CompareIgnoreCamelOrKebabCase {

        @Test
        void twoLiteralTest() {
            assertThat(CaseUtils.compareIgnoreCamelOrKebabCase("test-name", "testName")).isEqualTo(true);
        }

        @Test
        void singleLetterLiterals() {
            assertThat(CaseUtils.compareIgnoreCamelOrKebabCase("test-name-a-b", "testNameAB")).isEqualTo(true);
        }

        @Test
        void onlyCamelCase() {
            assertThat(CaseUtils.compareIgnoreCamelOrKebabCase("testNameAB", "testNameAB")).isEqualTo(true);
        }

        @Test
        void noCaseAndCamelCase() {
            assertThat(CaseUtils.compareIgnoreCamelOrKebabCase("testnameab", "testNameAB")).isEqualTo(true);
        }

        @Test
        void dividedByFullStop() {
            assertThat(CaseUtils.compareIgnoreCamelOrKebabCase("my-path.example-class", "myPath.exampleClass")).isEqualTo(true);
        }
    }
}
