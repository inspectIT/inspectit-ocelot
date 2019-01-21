package rocks.inspectit.oce.core.instrumentation;

import java.util.Set;

/**
 * Listener invoked when new classes have been loaded and discovered.
 */
public interface IClassDiscoveryListener {

    /**
     * It is guaranteed that for every class loaded by the JVM this method will be eventually called.
     *
     * @param newClasses the set of newly discovered classes
     */
    void newClassesDiscovered(Set<Class<?>> newClasses);
}
