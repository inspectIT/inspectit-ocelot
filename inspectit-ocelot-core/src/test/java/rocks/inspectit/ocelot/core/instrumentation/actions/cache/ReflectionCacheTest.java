package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ReflectionCacheTest {

    private static final String fieldValue = "test";

    private ReflectionCache reflectionCache;

    @BeforeEach
    void beforeEach() {
        reflectionCache = new ReflectionCache();
    }

    @Test
    void shouldReturnHiddenFieldValue() throws Exception {
        DummyClass dummy = new DummyClass();

        Object result = reflectionCache.getFieldValue(DummyClass.class, dummy, "field");

        assertThat(result).isEqualTo(fieldValue);
    }

    @Test
    void shouldReturnHiddenStaticFieldValue() throws Exception {
        Object result = reflectionCache.getFieldValue(DummyClass.class, null, "staticField");

        assertThat(result).isEqualTo(fieldValue);
    }

    @Test
    void shouldThrowExceptionForMissingField() {
        DummyClass dummy = new DummyClass();

        assertThatThrownBy(() -> reflectionCache.getFieldValue(DummyClass.class, dummy, "missing"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void shouldReturnResultOfInvokedMethod() throws Exception {
        DummyClass dummy = new DummyClass();
        String argument = "hello";

        Object result = reflectionCache.invokeMethod(DummyClass.class, dummy, "greet", argument);

        assertThat(result).isEqualTo(argument);
    }

    @Test
    void shouldReturnResultOfInvokedStaticMethod() throws Exception {
        Object result = reflectionCache.invokeMethod(DummyClass.class, null, "zero");

        assertThat(result).isEqualTo(0);
    }

    @Test
    void shouldReturnNullForInvokedVoidMethod() throws Exception {
        DummyClass dummy = new DummyClass();

        Object result = reflectionCache.invokeMethod(DummyClass.class, dummy, "empty");

        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionForMissingMethod() {
        DummyClass dummy = new DummyClass();

        // Wrong name
        assertThatThrownBy(() -> reflectionCache.invokeMethod(DummyClass.class, dummy, "missing"))
                .isInstanceOf(NoSuchMethodException.class);

        // Too many arguments
        assertThatThrownBy(() -> reflectionCache.invokeMethod(DummyClass.class, dummy, "greet", "arg1", "arg2"))
                .isInstanceOf(NoSuchMethodException.class);

        // Wrong argument type
        assertThatThrownBy(() -> reflectionCache.invokeMethod(DummyClass.class, dummy, "greet",0))
                .isInstanceOf(NoSuchMethodException.class);
    }

    static class DummyClass {

        private final String field = fieldValue;

        private final static String staticField = fieldValue;

        private String greet(String name) {
            return name;
        }

        private static int zero() {
            return 0;
        }

        private void empty() {}
    }
}
