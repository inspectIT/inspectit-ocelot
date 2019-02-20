package rocks.inspectit.oce.core.instrumentation.config.dummy;

@FunctionalInterface
public interface FunctionalInterfaceWithDefaultMethod {

    default String a() {
        return "a";
    }

    String b();
}
