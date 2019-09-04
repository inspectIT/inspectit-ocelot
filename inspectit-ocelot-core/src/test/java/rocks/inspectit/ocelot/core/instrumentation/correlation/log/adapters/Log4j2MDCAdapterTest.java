package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4j2MDCAdapterTest {

    public static class DummyMDC {

        static final Map<String, String> contents = new HashMap<>();

        static void put(String key, String value) {
            contents.put(key, value);
        }

        static String get(String key) {
            return contents.get(key);
        }

        static void remove(String key) {
            contents.remove(key);
        }

        static void reset() {
            contents.clear();
        }
    }

    private Log4J2MDCAdapter adapter;

    @BeforeEach
    void setup() {
        DummyMDC.reset();
        adapter = Log4J2MDCAdapter.get(DummyMDC.class);
    }

    @Nested
    class Set {

        @Test
        void ensureValuesPlaced() {
            DummyMDC.put("myKey", "someValue");

            adapter.set("myKey", "newValue");

            assertThat(DummyMDC.get("myKey")).isEqualTo("newValue");
        }

        @Test
        void ensureValuesRemoved() {
            DummyMDC.put("myKey", "someValue");

            adapter.set("myKey", null);

            assertThat(DummyMDC.contents.containsKey("myKey")).isFalse();
        }


        @Test
        void ensureUndoPreservesPreviousValue() {
            DummyMDC.put("myKey", "someValue");

            adapter.set("myKey", null).undoChange();

            assertThat(DummyMDC.get("myKey")).isEqualTo("someValue");
        }

        @Test
        void ensureUndoPreservesPreviousNull() {
            adapter.set("myKey", "something").undoChange();

            assertThat(DummyMDC.contents.containsKey("myKey")).isFalse();
        }

    }
}
