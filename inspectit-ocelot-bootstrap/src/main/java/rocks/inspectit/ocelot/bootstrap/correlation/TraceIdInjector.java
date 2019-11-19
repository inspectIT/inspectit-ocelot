package rocks.inspectit.ocelot.bootstrap.correlation;

public interface TraceIdInjector {

    Object injectTraceId(Object message);
}
