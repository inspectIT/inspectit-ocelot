package rocks.inspectit.ocelot.core.instrumentation.injection;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.objenesis.instantiator.util.ClassDefinitionUtils;
import org.springframework.stereotype.Component;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Injects provided bytecode into arbitrary target classloaders.
 * As injection has become harder since Java 9, it is required that a known neighbor class is given from the target
 * package. This also restricts that the injected class must be from the same package.
 */
@Component
@Slf4j
public class ClassInjector {

    /**
     * The instrumentation is required to enable "reusing" of previously injected but unused class objects using redefine.
     */
    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private JigsawModuleInstrumenter moduleManager;

    private InjectionClassLoader bootstrapChildLoader = new InjectionClassLoader();


    private Random nameGenerationRandom = new Random();

    /**
     * For each class loader this map holds a map, mapping a "structural identifier" to orphan classes.
     * To see what a structural identifier is, see the docu of {@link #inject(String, Class, ByteCodeProvider)}.
     * When a class that previously was injected gets unused, the {@link InjectedClass} is garbage collected.
     * However, as the actual class object is part of the target classloader, it does not get unloaded until the classlaoder does.
     * Therefore, to prevent creating new classes over and over again, we detect such "orphan" classes and reuse them using
     * {@link Instrumentation#redefineClasses(ClassDefinition...)}.
     * Every found orphan class is registered in this map.
     * <p>
     * We never directly inject classes to the bootstrap. Instead, we use our {@link #bootstrapChildLoader}.
     */
    private WeakHashMap<ClassLoader, Map<String, LinkedList<WeakReference<Class<?>>>>> orphanClasses = new WeakHashMap<>();

    /**
     * With this reference queue we detected that an {@link InjectedClass} has been garbage collected, meaning that the underlying class object is now an orphan.
     * We process this queue and add all found orphans to {@link #orphanClasses}.
     */
    private ReferenceQueue<InjectedClass<?>> unusedInjectedClassesQueue = new ReferenceQueue<>();

    /**
     * We need to store our {@link InjectedClassReference} to be notified when a {@link InjectedClass} is garbage collected.
     * Without this set, {@link InjectedClassReference}s get garbage collected immediately and {@link #unusedInjectedClassesQueue} never gets notified.
     */
    private Set<InjectedClassReference> activeReferences = new HashSet<>();

    /**
     * Injects a custom class into a target classloader.
     * A unique name for the target class is automatically generated.
     * If possible, this method tries to reuse an orphan class which was previously injected into the given classloader.
     * <p>
     * A class is considered to be an orphan as soon as the {@link InjectedClass} returned by this method is garbage collected.
     * Therefore NEVER EVER store and use the class object {@link InjectedClass#getInjectedClassObject()} without also storing the {@link InjectedClass} instance.
     * <p>
     * NOTE: If the neighborClass comes from the bootstrap, the classes are not actually added to the bootstrap.
     * Instead a child-classloader is used.
     *
     * @param classStructureIdentifier A symbolic identifier for the "structure" of the class to inject. It must be guaranteed that two injected classes with the same "classStructureIdentifier" could be redefined into each other.
     *                                 This means that as described for {@link Instrumentation#redefineClasses(ClassDefinition...)}, the classes must have the same methods (including their signatures), fields and all modifieres must be the same.
     * @param neighborClass            a class which resides in the classloader to which the class shall be injected.
     * @param byteCodeGenerator        the bytecode of the class to inject
     * @return the class which has been injected
     * @throws Exception if an exception occurred during the injection or during the invocation of byteCodeGenerator, this exception is passed through
     */
    public synchronized InjectedClass<?> inject(String classStructureIdentifier, Class<?> neighborClass, ByteCodeProvider byteCodeGenerator) throws Exception {

        //check if we can reuse an existing class instead of injecting a new one
        Optional<Class<?>> classToReuse = tryReusingClassInLoader(classStructureIdentifier, neighborClass.getClassLoader());

        try {
            String className = classToReuse.map(Class::getName).orElse(getUniqueNameInSamePackage(neighborClass));
            byte[] byteCode = byteCodeGenerator.generateBytecode(className);
            InjectedClass<?> result;
            Class<?> resultClass;
            if (classToReuse.isPresent()) {
                resultClass = classToReuse.get();
                ClassDefinition def = new ClassDefinition(resultClass, byteCode);
                log.debug("Reusing orphan generated class {}", className);
                instrumentation.redefineClasses(def);
                result = new InjectedClass<>(resultClass);
            } else {
                log.debug("Injecting new class {}", className);
                moduleManager.openModule(neighborClass);
                resultClass = injectClass(neighborClass, className, byteCode);
                result = new InjectedClass<Object>(resultClass);
            }
            //This reference lets us no when all references to the InjectedClass object are lost
            // as soon as this happens we assume that the underlying class can be recycled
            activeReferences.add(new InjectedClassReference(classStructureIdentifier, result, resultClass, unusedInjectedClassesQueue));
            return result;
        } catch (Throwable t) {
            classToReuse.ifPresent(clazz -> markClassForReuse(classStructureIdentifier, clazz));
            throw t;
        }
    }

    private Class<?> injectClass(Class<?> neighborClass, String className, byte[] byteCode) throws Exception {
        ClassLoader loader = neighborClass.getClassLoader();
        if (loader == null) {
            //for bootstrap classes the standard injection does not work properly
            //however it also is not necessary as a reference to the bootstrap loader won't cause a memoryleak anyway
            return bootstrapChildLoader.defineNewClass(className, byteCode);
        } else {
            return ClassDefinitionUtils.defineClass(className, byteCode, neighborClass, loader);
        }
    }

    private String getUniqueNameInSamePackage(Class<?> neighborClass) {

        String packagePrefix;
        if (neighborClass.getClassLoader() == null) {
            //when injecting to the "bootstrap" we actually inject to #bootstrapChildLoader
            //therefore, we must not reuse any java.* package names
            packagePrefix = "rocks.inspectit.ocelot.injected.";
        } else {
            packagePrefix = neighborClass.getName();
            int lastDot = packagePrefix.lastIndexOf('.');
            packagePrefix = packagePrefix.substring(0, lastDot + 1);
            if (packagePrefix.isEmpty()) {
                throw new RuntimeException("Injection into the default package is not supported!");
            }
        }

        String namePrefix = packagePrefix + "inspectitGen$$$";
        ClassLoader loader = neighborClass.getClassLoader();

        while (true) {
            String name = namePrefix + Math.abs(nameGenerationRandom.nextLong());
            try {
                Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                return name;
            }
        }
    }

    private void markClassForReuse(String classStructureIdentifier, Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        orphanClasses.computeIfAbsent(loader, (cl) -> new HashMap<>())
                .computeIfAbsent(classStructureIdentifier, (cl) -> new LinkedList<>())
                .add(new WeakReference<>(clazz));
    }

    private Optional<Class<?>> tryReusingClassInLoader(String classStructureIdentifier, ClassLoader loader) {
        if (loader == null) {
            loader = bootstrapChildLoader;
        }
        collectOrphanClasses();
        LinkedList<WeakReference<Class<?>>> orphansOfGivenStructureInClassloader =
                orphanClasses.getOrDefault(loader, Collections.emptyMap())
                        .getOrDefault(classStructureIdentifier, new LinkedList<>());

        WeakReference<Class<?>> classToReuse = orphansOfGivenStructureInClassloader.pollFirst();
        if (classToReuse == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(classToReuse.get());
        }
    }

    private void collectOrphanClasses() {
        Reference<? extends InjectedClass<?>> freedRef = unusedInjectedClassesQueue.poll();
        while (freedRef != null) {
            val classRef = (InjectedClassReference) freedRef;
            activeReferences.remove(classRef);
            Class<?> injectedClass = classRef.injectedClassObject.get();
            //the target classloader might have been garbage collected, therefore the null check
            if (injectedClass != null) {
                //allow reuse of this class
                markClassForReuse(classRef.classStructureIdentifier, injectedClass);
            }
            freedRef = unusedInjectedClassesQueue.poll();
        }
    }

    @FunctionalInterface
    public interface ByteCodeProvider {
        /**
         * The function providing the bytecode which shall be injected.
         * The name is provided by the caller which (a) ensures that it is unique and (b) that the class gets put in a package in which classes can be injected.
         *
         * @param className the name of the class to use in the bytecode.
         * @return the bytecode to use
         * @throws Exception if something went wrong during the bytecode generation, this exception is passed through by {@link #inject(String, Class, ByteCodeProvider)}
         */
        byte[] generateBytecode(String className) throws Exception;
    }

    /**
     * A specialized {@link WeakReference} to an {@link InjectedClass}, which preserves the actual class object and its structural identifier,
     * even when the {@link InjectedClass} is garbage collected.
     */
    private static class InjectedClassReference extends WeakReference<InjectedClass<?>> {

        private final WeakReference<Class> injectedClassObject;
        private final String classStructureIdentifier;

        public InjectedClassReference(String classStructureIdentifier, InjectedClass<?> referent, Class<?> targetClass, ReferenceQueue<? super InjectedClass<?>> q) {
            super(referent, q);
            this.classStructureIdentifier = classStructureIdentifier;
            injectedClassObject = new WeakReference<>(targetClass);
        }
    }

    private static class InjectionClassLoader extends ClassLoader {

        public Class<?> defineNewClass(String className, byte[] code) throws Exception {
            super.defineClass(className, code, 0, code.length);
            return Class.forName(className, false, this);
        }
    }
}
