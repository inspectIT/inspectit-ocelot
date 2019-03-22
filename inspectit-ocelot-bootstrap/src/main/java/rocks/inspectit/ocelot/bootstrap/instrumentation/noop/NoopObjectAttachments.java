package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.instrumentation.IObjectAttachments;

public class NoopObjectAttachments implements IObjectAttachments {

    public static final NoopObjectAttachments INSTANCE = new NoopObjectAttachments();

    @Override
    public void attach(Object target, String key, Object value) {
    }

    @Override
    public Object getAttachment(Object target, String key) {
        return null;
    }
}
