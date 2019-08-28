package rocks.inspectit.ocelot.core.config.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseUtilTest {

    @Test
    void testCamelCaseToKebabCase() {
        assertEquals(CaseUtils.camelCaseToKebabCase("testName"), "test-name");
        assertEquals(CaseUtils.camelCaseToKebabCase("testNameAB"), "test-name-a-b");
        assertEquals(CaseUtils.camelCaseToKebabCase("test"), "test");
        assertEquals(CaseUtils.camelCaseToKebabCase("myPath.exampleClass"), "my-path.example-class");
    }

    @Test
    void testKebabCaseToCamelCase() {
        assertEquals(CaseUtils.kebabCaseToCamelCase("test-name"), "testName");
        assertEquals(CaseUtils.kebabCaseToCamelCase("test-name-a-b"), "testNameAB");
        assertEquals(CaseUtils.kebabCaseToCamelCase("test"), "test");
        assertEquals(CaseUtils.kebabCaseToCamelCase("my-path.example-class"), "myPath.exampleClass");
    }
}
