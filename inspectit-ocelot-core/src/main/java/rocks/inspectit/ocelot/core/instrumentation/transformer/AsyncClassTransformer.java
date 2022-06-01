package rocks.inspectit.ocelot.core.instrumentation.transformer;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.ocelot.core.instrumentation.special.ClassLoaderDelegation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * Async {@link ClassTransformer} implementation. This is the default implementation to avoid unnecessary blocking
 * at application boot time.
 */
@Component
@Slf4j
public class AsyncClassTransformer extends AbstractClassTransformer {

    @Autowired
    @VisibleForTesting
    List<IClassDefinitionListener> classDefinitionListeners;

    @Autowired
    private ClassLoaderDelegation classLoaderDelegation;

    @Override
    public boolean isEnabled() {
        return env.getCurrentConfig().getInstrumentation().getInternal().isAsync();
    }

    @Override
    public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        if (classBeingRedefined == null) { // class is not loaded yet! we redefine only loaded classes to prevent blocking
            classDefinitionListeners.forEach(lis -> lis.onNewClassDefined(className, loader));
            return bytecode; //leave the class unchanged for now
        }

        //retransform can be triggered by other agents where the classloader delegation has not been applied yet
        if (!classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(loader, configResolver.getCurrentConfig())
                .isEmpty()) {
            log.debug("Skipping instrumentation of {} as bootstrap classes were not made available yet for the class", className);
            return bytecode; //leave the class unchanged for now
        } else {
            return instrumentByteCode(TypeDescriptionWithClassLoader.of(classBeingRedefined), classBeingRedefined, bytecode, updateAndGetActiveConfiguration(classBeingRedefined));
        }
    }

}
