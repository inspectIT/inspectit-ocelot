package rocks.inspectit.oce.bootstrap.instrumentation;

public class ClassLoaderDelegationMarker {

    public static final ThreadLocal<Boolean> CLASS_LOADER_DELEGATION_PERFORMED = new ThreadLocal<>();
}
