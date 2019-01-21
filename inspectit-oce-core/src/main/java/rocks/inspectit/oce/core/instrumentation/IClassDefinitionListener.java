package rocks.inspectit.oce.core.instrumentation;

import java.security.ProtectionDomain;

/**
 * Spring beans implementing this interfaces will get notified when a new class definition has been encountered.
 * These listeners are invoked when {@link java.lang.instrument.ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])}
 * has been invoked with a null class-isntance.
 */
public interface IClassDefinitionListener {

    /**
     * Invoked when {@link java.lang.instrument.ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])} was called with a null Class.
     *
     * @param className the className provided to ClassFileTransformer.transform
     * @param loader    the loader provided to ClassFileTransformer.transform
     */
    void newClassDefined(String className, ClassLoader loader);
}
