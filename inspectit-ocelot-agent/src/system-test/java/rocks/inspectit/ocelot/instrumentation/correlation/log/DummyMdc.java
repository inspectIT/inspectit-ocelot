package rocks.inspectit.ocelot.instrumentation.correlation.log;

import java.util.HashMap;
import java.util.Map;

public class DummyMdc {

    private static ThreadLocal<Map<String, String>> mapThreadLocal = ThreadLocal.withInitial(HashMap::new);

    public static void put(String key, String val) {
        mapThreadLocal.get().put(key, val);
    }

    public static String get(String key) {
        return mapThreadLocal.get().get(key);
    }

    public static void remove(String key) {
        mapThreadLocal.get().remove(key);
    }

    public static void reset() {
        mapThreadLocal = ThreadLocal.withInitial(HashMap::new);
    }
}
