package rocks.inspectit.ocelot.core.instrumentation.config.dummy;

public interface InterfaceWithDefaultMethod {

    default String a() {
        return "a";
    }
}
