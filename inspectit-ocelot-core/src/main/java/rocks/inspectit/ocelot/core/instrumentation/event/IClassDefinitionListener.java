package rocks.inspectit.ocelot.core.instrumentation.event;

import java.security.ProtectionDomain;

/**
 * Spring beans implementing this interfaces will get notified when a new class definition has been encountered.
 * These listeners are invoked when {@link java.lang.instrument.ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])}
 * has been invoked with a null class-instance.
 */
public interface IClassDefinitionListener {

    /**
     * Invoked when {@link java.lang.instrument.ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])}
     * was called with a null Class.
     * WARNING: This method is called directly from a {@link java.lang.instrument.ClassFileTransformer}.
     * Therefore the classloading cannot continue until this listener has completed! Listeners should therefore be as fast as possible.
     *
     * @param className the className provided to ClassFileTransformer.transform
     * @param loader    the loader provided to ClassFileTransformer.transform
     */
    void onNewClassDefined(String className, ClassLoader loader);
}
