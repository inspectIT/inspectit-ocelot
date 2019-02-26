package rocks.inspectit.oce.core.instrumentation.config.dummy;

public interface InterfaceWithDefaultMethod {

    default String a() {
        return "a";
    }
}
