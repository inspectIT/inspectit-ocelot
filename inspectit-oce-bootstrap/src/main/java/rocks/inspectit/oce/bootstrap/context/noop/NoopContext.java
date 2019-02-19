package rocks.inspectit.oce.bootstrap.context.noop;

import rocks.inspectit.oce.bootstrap.context.IInspectitContext;

import java.util.Collections;
import java.util.Map;

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
}
