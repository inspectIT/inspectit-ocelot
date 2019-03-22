package rocks.inspectit.ocelot.instrumentation;

interface NamedElement {

    default void doSomething(Runnable r) {
        r.run();
    }

    String getName();
}
