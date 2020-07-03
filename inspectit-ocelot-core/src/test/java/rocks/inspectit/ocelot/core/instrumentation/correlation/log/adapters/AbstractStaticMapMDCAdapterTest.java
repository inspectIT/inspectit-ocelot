package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.utils.WeakMethodReference;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractStaticMapMDCAdapterTest {

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

    private AbstractStaticMapMDCAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        DummyMDC.reset();
        WeakMethodReference put = WeakMethodReference.create(DummyMDC.class, "put", String.class, String.class);
        WeakMethodReference get = WeakMethodReference.create(DummyMDC.class, "get", String.class);
        WeakMethodReference remove = WeakMethodReference.create(DummyMDC.class, "remove", String.class);
        adapter = new AbstractStaticMapMDCAdapter(put, get, remove) {
            @Override
            public boolean isEnabledForConfig(TraceIdMDCInjectionSettings settings) {
                return true;
            }
        };
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

            adapter.set("myKey", null).close();

            assertThat(DummyMDC.get("myKey")).isEqualTo("someValue");
        }

        @Test
        void ensureUndoPreservesPreviousNull() {
            adapter.set("myKey", "something").close();

            assertThat(DummyMDC.contents.containsKey("myKey")).isFalse();
        }

    }
}
