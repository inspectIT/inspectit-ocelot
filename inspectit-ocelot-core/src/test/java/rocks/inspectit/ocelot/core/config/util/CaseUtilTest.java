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
}
