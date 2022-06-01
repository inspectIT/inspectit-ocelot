package rocks.inspectit.ocelot.core.instrumentation.transformer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDefinitionListener;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * Synchronous {@link ClassTransformer} implementation.
 * <p>
 * Can be enabled with  property <i>inspectit.instrumentation.internal.async</i>
 * </p>
 *
 * @see rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings
 */
@Component
@Slf4j
public class SyncClassTransformer extends AbstractClassTransformer {

    @Autowired
    @VisibleForTesting
    List<IClassDefinitionListener> classDefinitionListeners;

    /**
     * This Cache holds all {@link ClassInstrumentationConfiguration} and the corresponding bytecode for all classes
     * which are instrumented on initial class load.
     * Since all {@link rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener} are invoked after instrumentation
     * we ensure {@link #doTransform(ClassLoader, String, Class, ProtectionDomain, byte[])} is invoked again and we can
     * dispatch a new {@link rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent} with the actual
     * class object.
     */
    Cache<CacheKey, CacheEntry> temporaryInstrumentationConfigCache = CacheBuilder.newBuilder().build();

    @Override
    public boolean isEnabled() {
        return !env.getCurrentConfig().getInstrumentation().getInternal().isAsync();
    }

    @Override
    protected void deinstrumentAllClasses() {
        // Attach all class which might still be in the temp cache and not yet handled by the InstrumentationTrigger
        if (temporaryInstrumentationConfigCache.size() > 0) {
            temporaryInstrumentationConfigCache.asMap().keySet().forEach(k -> {
                try {
                    instrumentedClasses.put(Class.forName(k.getClassName()), Boolean.TRUE);
                } catch (ClassNotFoundException e) {
                    //Should never happen since we saw this class loading
                }
            });
            temporaryInstrumentationConfigCache.invalidateAll();
        }
        super.deinstrumentAllClasses();
    }

    @Override
    public byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {

        byte[] instrumentedBytecode;
        String classNameInDotNotation = className.replace("/", ".");

        CacheKey cacheKey = new CacheKey(loader, classNameInDotNotation);
        ClassInstrumentationConfiguration classConf;
        TypeDescriptionWithClassLoader typeWithLoader;

        if (classBeingRedefined == null) { // Initial class load
            typeWithLoader = TypeDescriptionWithClassLoader.of(classNameInDotNotation, loader);
            classConf = configResolver.getClassInstrumentationConfiguration(typeWithLoader);
            instrumentedBytecode = instrumentByteCode(typeWithLoader, null, bytecode, classConf);
            temporaryInstrumentationConfigCache.put(cacheKey, new CacheEntry(classConf, instrumentedBytecode));
            classDefinitionListeners.forEach(lis -> lis.onNewClassDefined(className, loader));
        } else {
            typeWithLoader = TypeDescriptionWithClassLoader.of(classBeingRedefined);
            classConf = updateAndGetActiveConfiguration(classBeingRedefined);

            CacheEntry entry = temporaryInstrumentationConfigCache.getIfPresent(cacheKey);
            temporaryInstrumentationConfigCache.invalidate(cacheKey);

            if (entry != null) {
                // we have seen this class before check if config differs and a retransformation is required
                if (!entry.getClassConfiguration().isSameAs(typeWithLoader, classConf)) {
                    instrumentedBytecode = instrumentByteCode(typeWithLoader, classBeingRedefined, bytecode, classConf);
                } else {
                    // reuse cached bytecode and notify InstrumentationManger about instrumentation
                    instrumentedBytecode = entry.getBytecode();
                    dispatchClassInstrumentedEvent(classBeingRedefined, typeWithLoader.getType(), classConf);
                }
            } else {
                instrumentedBytecode = instrumentByteCode(typeWithLoader, classBeingRedefined, bytecode, classConf);
            }
        }

        return instrumentedBytecode;
    }

    /**
     * Simple value object used as key for {@link #temporaryInstrumentationConfigCache} since class names are only
     * unique in combination with the corresponding classloader.
     */
    @Value
    private static class CacheKey {

        ClassLoader loader;

        String className;
    }

    /**
     * Simple value object holding a {@link  ClassInstrumentationConfiguration} and the resulting bytecode
     */
    @Value
    private static class CacheEntry {

        ClassInstrumentationConfiguration classConfiguration;

        byte[] bytecode;
    }
}
