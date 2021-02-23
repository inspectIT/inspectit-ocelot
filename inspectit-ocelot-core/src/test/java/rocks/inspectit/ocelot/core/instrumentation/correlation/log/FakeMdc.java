package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import java.util.HashMap;
import java.util.Map;

public class FakeMdc {

    private final Map<String, Object> content = new HashMap<>();

    public Map<String, Object> getContent() {
        return content;
    }

    public Object get(String key) {
        return content.get(key);
    }

    public void put(String key, Object value) {
        content.put(key, value);
    }

    public void remove(String key) {
        content.remove(key);
    }
}
