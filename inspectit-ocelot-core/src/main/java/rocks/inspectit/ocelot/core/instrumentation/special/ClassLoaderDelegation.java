package rocks.inspectit.ocelot.core.instrumentation.special;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.ClassLoaderDelegationMarker;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.core.instrumentation.InstrumentationTriggerer;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This component performs an instrumentation of {@link ClassLoader}s if.
 * This is required to make our bootstrap classes accessible in custom module systems, such as OSGi.
 * <p>
 * The instrumentation is triggered through the {@link InstrumentationTriggerer},
 * which invokes {@link #getClassLoaderClassesRequiringRetransformation(Class, LinkedHashSet)} to find out if any class loaders need tobe instrumented before instrumenting a class.
 * This is currently only used by the {@link InstrumentationTriggerer}.
 */
@Component
@Slf4j
public class ClassLoaderDelegation implements SpecialSensor, IClassDiscoveryListener {

    static {
        //ensure that this bootstrap class is loaded BEFORE any classloader is instrumented
        ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.get();
    }

    /**
     * Stores which classloaders have already been instrumented.
     * We never deinstrument a classloader after we instrument it, even if {@link SpecialSensorSettings#isClassLoaderDelegation()}
     * is changed to false.
     */
    private final InstrumentedClassLoaders instrumentedClassLoaders = new InstrumentedClassLoaders();

    private final ElementMatcher<MethodDescription> LOAD_CLASS_MATCHER = named("loadClass").and(takesArguments(String.class, boolean.class).or(takesArguments(String.class)));

    private final ElementMatcher<TypeDescription> CLASS_LOADER_MATCHER = isSubTypeOf(ClassLoader.class).and(not(nameStartsWith("java.")))
            .and(declaresMethod(LOAD_CLASS_MATCHER));

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        instrumentedClassLoaders.onNewClassesDiscovered(newClasses);
    }

    @Override
    public boolean shouldInstrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings) {

        boolean isClassLoader = CLASS_LOADER_MATCHER.matches(typeWithLoader.getType());
        boolean cldEnabled = settings.getSource().getSpecial().isClassLoaderDelegation();
        //we never de instrument a classloader we previously instrumented
        return instrumentedClassLoaders.contains(typeWithLoader) || (isClassLoader && cldEnabled);
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;
    }

    @Override
    public DynamicType.Builder instrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        //remember that this classloader was instrumented
        instrumentedClassLoaders.add(typeWithLoader);
        return builder.visit(Advice.to(ClassloaderDelegationAdvice.class).on(LOAD_CLASS_MATCHER));
    }

    public LinkedHashSet<Class<?>> getClassLoaderClassesRequiringRetransformation(ClassLoader classLoader, InstrumentationConfiguration config) {
        if (!config.getSource().getSpecial().isClassLoaderDelegation()) {
            return new LinkedHashSet<>(); //class loader delegation is disabled, nothing to instrument
        }
        if (classLoader == null) {
            return new LinkedHashSet<>(); //bootstrap loader does not need instrumentation
        }

        LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
        getClassLoaderClassesRequiringRetransformation(classLoader.getClass(), result);
        return result;
    }

    private void getClassLoaderClassesRequiringRetransformation(Class<?> classLoaderClass, LinkedHashSet<Class<?>> result) {
        if (result.contains(classLoaderClass)) {
            return; // this classloader class has already been handled
        }
        //the parent class could override loadClass, therefore we need to instrument it
        Class<?> superClass = classLoaderClass.getSuperclass();
        if (superClass != ClassLoader.class) {
            getClassLoaderClassesRequiringRetransformation(superClass, result);
        }

        TypeDescriptionWithClassLoader typeWithLoader = TypeDescriptionWithClassLoader.of(classLoaderClass);
        if (!instrumentedClassLoaders.contains(typeWithLoader) && CLASS_LOADER_MATCHER.matches(typeWithLoader.getType())) {
            ClassLoader loadingClassLoader = classLoaderClass.getClassLoader();
            if (loadingClassLoader != null) {
                getClassLoaderClassesRequiringRetransformation(loadingClassLoader.getClass(), result);
            }
            result.add(classLoaderClass);
        }
    }

    private static class ClassloaderDelegationAdvice {

        @Advice.OnMethodEnter(skipOn = Class.class)
        public static Class<?> enter(@Advice.Argument(value = 0) String classname) {
            //we have to hardcode the classname as the bytecode gets inlined
            if (classname != null && classname.startsWith("rocks.inspectit.ocelot.bootstrap")) {
                //some JVMs, such as IBM implement the bootstrap classloader as a non-native class
                //in this case we need to make sure that the classloader delegation is only performed once
                //because otherwise we end up with a StackOverflowError (the instrument bootstrap loaded would call
                //Class.forName in an infinite recursion)
                boolean cldPerformed = ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.get();
                if (!cldPerformed) {
                    ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.set(true);
                    try {
                        //delegate the loading to the bootstrap
                        return Class.forName(classname, false, null);
                    } catch (ClassNotFoundException e) {
                        //should never occur as we put our stuff on the bootstrap
                        return null; //execute the classloader normally
                    } finally {
                        ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.set(false);
                    }
                }
            }
            return null; //execute the classloader normally
        }

        @Advice.OnMethodExit()
        public static void exit(@Advice.Enter Class<?> resultClass, @Advice.Return(readOnly = false) Class<?> returnValue) {
            if (resultClass != null) {
                returnValue = resultClass;
            }
        }
    }

    /**
     * Utility class to keep track of implemented ClassLoaders.
     * We need a two step caching here to support instrumenting ClassLoaders on first load, hence we have no class to cache with weak keys.
     * This will only happen if async instrumentation is disabled!
     * If the instrumented ClassLoader is finally loaded it is move to a final cache to ensure unloaded ClassLoaders will be removed from cache
     * and on reload the same ClassLoader it will be instrumented again.
     */
    @Slf4j
    private static class InstrumentedClassLoaders implements IClassDiscoveryListener {

        /**
         * Temporary cache to store instrumented ClassLoader class names with the related ClassLoader
         */
        private final Set<Pair<String, ClassLoader>> instrumentedClassLoadersByName = Collections.newSetFromMap(new ConcurrentHashMap<>());

        /**
         * Final cache to store weak references of the instrumented ClassLoaders
         */
        private final Set<Class<?>> instrumentedClassLoaderClasses = Collections.newSetFromMap(CacheBuilder.newBuilder()
                .weakKeys()
                .<Class<?>, Boolean>build()
                .asMap());

        public boolean contains(TypeDescriptionWithClassLoader typeWithLoader) {
            return instrumentedClassLoadersByName.contains(toPair(typeWithLoader)) || instrumentedClassLoaderClasses.contains(typeWithLoader.getRelatedClass());
        }

        public void add(TypeDescriptionWithClassLoader typeWithLoader) {
            if (typeWithLoader.getRelatedClass() != null) {
                instrumentedClassLoaderClasses.add(typeWithLoader.getRelatedClass());
            } else {
                instrumentedClassLoadersByName.add(toPair(typeWithLoader));
            }
        }

        /**
         * Moves all instrumented ClassLoaders from {@link #instrumentedClassLoadersByName} to {@link #instrumentedClassLoaderClasses}
         *
         * @param newClasses the set of newly discovered classes
         */
        @Override
        public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
            newClasses.forEach(clazz -> {
                Pair<String, ClassLoader> tmp = Pair.of(clazz.getName(), clazz.getClassLoader());
                if (instrumentedClassLoadersByName.contains(tmp)) {
                    instrumentedClassLoaderClasses.add(clazz);
                    instrumentedClassLoadersByName.remove(tmp);
                }
            });
            if (log.isDebugEnabled()) {
                if (instrumentedClassLoadersByName.size() > 0) {
                    log.debug("Pending ClassLoaders to be transferred to final class cache:{}", instrumentedClassLoadersByName.stream()
                            .map(Pair::getLeft)
                            .collect(Collectors.joining(", ")));
                }
            }
        }

        private Pair<String, ClassLoader> toPair(TypeDescriptionWithClassLoader typeWithLoader) {
            return Pair.of(typeWithLoader.getName(), typeWithLoader.getLoader());
        }
    }

}
