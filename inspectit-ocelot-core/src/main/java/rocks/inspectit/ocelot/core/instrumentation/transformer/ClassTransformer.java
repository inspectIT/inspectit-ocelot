package rocks.inspectit.ocelot.core.instrumentation.transformer;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Internal interface representing a {@link java.lang.instrument.ClassFileTransformer}
 */
public interface ClassTransformer {

    /**
     * @return true, if transformer is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Pre Java9 ClassFileTransformer#transform implementation
     *
     * @see java.lang.instrument.ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])
     */
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // delegate to our Java 9 compatible transform method
        return transform(null, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

    /**
     * After Java9+ ClassFileTransformer#transform implementation
     * <p>
     * Java 9 Signature:  java.lang.instrument.ClassFileTransformer#transform(Module, ClassLoader, String, Class, ProtectionDomain, byte[])
     */
    byte[] transform(Object module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException;

    /**
     * Destroys the {@link java.lang.instrument.ClassFileTransformer}.
     */
    void destroy();
}
