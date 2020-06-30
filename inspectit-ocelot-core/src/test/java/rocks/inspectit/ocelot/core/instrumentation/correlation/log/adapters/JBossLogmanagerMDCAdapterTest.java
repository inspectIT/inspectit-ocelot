package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JBossLogmanagerMDCAdapterTest {

    private static class DummyMDC {

        private static Map<String, String> backingMap = new HashMap<>();

        public static String get(String key) {
            return backingMap.get(key);
        }

        public static String put(String key, String value) {
            return backingMap.put(key, value);
        }

        public static String remove(String key) {
            return backingMap.remove(key);
        }
    }

    @Nested
    public static class Set {

        @Test
        void verifyMDCAccess() {
            DummyMDC.put("my_key", "default");
            JBossLogmanagerMDCAdapter adapter = JBossLogmanagerMDCAdapter.get(DummyMDC.class);

            MDCAccess.Undo undo = adapter.set("my_key", "overridden");
            assertThat(DummyMDC.get("my_key")).isEqualTo("overridden");

            undo.close();
            assertThat(DummyMDC.get("my_key")).isEqualTo("default");

        }
    }

}
