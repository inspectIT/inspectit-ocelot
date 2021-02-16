package rocks.inspectit.ocelot.bootstrap.correlation;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;

public abstract class MdcAccessor implements DoNotInstrumentMarker {

    public abstract Object get(String key);

    public abstract void put(String key, Object value);

    public abstract void remove(String key);
}
