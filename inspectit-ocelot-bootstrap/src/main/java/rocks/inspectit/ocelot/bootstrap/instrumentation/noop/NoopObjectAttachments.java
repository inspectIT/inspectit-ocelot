package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.ObjectAttachments;

public class NoopObjectAttachments implements ObjectAttachments {

    public static final NoopObjectAttachments INSTANCE = new NoopObjectAttachments();

    @Override
    public Object attach(Object target, String key, Object value) {
        return null;
    }

    @Override
    public Object getAttachment(Object target, String key) {
        return null;
    }
}
