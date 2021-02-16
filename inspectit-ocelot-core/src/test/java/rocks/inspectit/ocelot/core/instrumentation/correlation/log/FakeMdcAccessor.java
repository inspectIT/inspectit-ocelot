package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import java.util.HashMap;
import java.util.Map;

public class FakeMdcAccessor extends MdcAccessor {

    private final Map<String, Object> content = new HashMap<>();

    public Map<String, Object> getContent() {
        return content;
    }

    @Override
    public Object get(String key) {
        return content.get(key);
    }

    @Override
    public void put(String key, Object value) {
        content.put(key, value);
    }

    @Override
    public void remove(String key) {
        content.remove(key);
    }
}
