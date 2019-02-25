package rocks.inspectit.oce.instrumentation;

interface NamedElement {

    default void doSomething(Runnable r) {
        r.run();
    }

    String getName();
}
