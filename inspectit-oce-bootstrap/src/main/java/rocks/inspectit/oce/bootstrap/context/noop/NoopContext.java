package rocks.inspectit.oce.bootstrap.context.noop;

import rocks.inspectit.oce.bootstrap.context.IInspectitContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NoopContext implements IInspectitContext {

    public static final NoopContext INSTANCE = new NoopContext();

    private NoopContext() {
    }

    @Override
    public void setData(String key, Object value) {
    }

    @Override
    public Object getData(String key) {
        return null;
    }

    @Override
    public Iterable<Map.Entry<String, Object>> getData() {
        return Collections.<String, Object>emptyMap().entrySet();
    }

    @Override
    public void makeActive() {
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, String> getDownPropagationHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getUpPropagationHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public void readPropagationHeaders(Map<String, String> headers) {

    }

    @Override
    public Set<String> getPropagationHeaderFields() {
        return Collections.emptySet();
    }
}
