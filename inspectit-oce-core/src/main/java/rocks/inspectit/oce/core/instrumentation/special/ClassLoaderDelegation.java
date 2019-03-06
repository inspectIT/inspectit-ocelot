package rocks.inspectit.oce.core.instrumentation.special;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * This component performs an instrumentation of {@link ClassLoader}s if they do not give access to the classes we publish on the bootstrap classloader.
 * This is often the case with custom module systems, such as OSGi.
 * <p>
 * The process of making the bootstrap classes available has to be manually triggered by calling {@link #makeBootstrapClassesAvailable(ClassLoader)}.
 * This is currently only used by the {@link rocks.inspectit.oce.core.instrumentation.InstrumentationTriggerer}.
 */
@Component
@Slf4j
public class ClassLoaderDelegation {

    /**
     * This class is used to check if the bootstrap is accessible.
     */
    private static final Class<?> PROBE_CLASS = Instances.class;

    /**
     * A cache for remembering on which classloaders we tried to make our bootstrap classes available.
     * This cache stores the result: if the value is "true", our bootstrap classes wehre available by default or we applied
     * a successful classloader delegation instrumentation.
     * If the stored value is "false" we tried to apply a classloader delegation but were not successful. In this
     * case the bootstrap classes are NOT available, therefore any class in this classloader must not be instrumented / hooked.
     */
    private final Cache<ClassLoader, Boolean> bootstrapClassesAccessible = CacheBuilder.newBuilder().weakKeys().build();

    /**
     * Stores the ClassLoader classes on which we want to activate the classloader delegation.
     * This set is used by the {@link ClassloaderDelegationSensor} to check if the sensor is active for a given class.
     */
    private final Set<Class<? extends ClassLoader>> classLoadersToInstrument = Collections.newSetFromMap(
            CacheBuilder.newBuilder().weakKeys().<Class<? extends ClassLoader>, Boolean>build().asMap());

    @Autowired
    Instrumentation instrumentation;

    /**
     * Checks if a call to {@link #makeBootstrapClassesAvailable(ClassLoader)} for the given classloader was issued
     * and the bootstrap classes are available in the given classloader.
     *
     * @param classLoader the classloader to check
     * @return true, if (a) a successful classloader delegation was applied via {@link #makeBootstrapClassesAvailable(ClassLoader)}
     * or (b) {@link #makeBootstrapClassesAvailable(ClassLoader)} was called and the bootstrap classes were already available
     */
    public boolean wasBootstrapMadeAvailableTo(ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        Boolean isBootstrapAccessible = bootstrapClassesAccessible.getIfPresent(classLoader);
        return isBootstrapAccessible != null && isBootstrapAccessible;
    }

    /**
     * Tries to make the inspectIT bootstrap classes such as {@link Instances} available to the given classloader.
     * This method first checks if the classes are available by default.
     * If this is not the case, it tries to instrument the classloader with the classloader delegation via {@link ClassloaderDelegationSensor}.
     * <p>
     * The result of this method invocation is cached, meaning that repeated calls cause no overhead.
     *
     * @param classLoader the classloader through which our bootstrap classes should be available.
     * @return true, if the bootstrap classes are now available to the given classloader. False otherwise, in
     * which case no instrumentation should be applied to any class loaded by this classloader.
     */
    public boolean makeBootstrapClassesAvailable(ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }

        Boolean isBootstrapAccessible = bootstrapClassesAccessible.getIfPresent(classLoader);
        if (isBootstrapAccessible == null) {
            return makeBootstrapClassesAvailableSynchronized(classLoader);
        } else {
            return isBootstrapAccessible;
        }
    }

    private synchronized boolean makeBootstrapClassesAvailableSynchronized(ClassLoader classLoader) {
        Boolean isBootstrapAccessible = bootstrapClassesAccessible.getIfPresent(classLoader);
        if (isBootstrapAccessible != null) {
            return isBootstrapAccessible;
        }

        isBootstrapAccessible = areBootstrapClassesAvailable(classLoader);
        if (!isBootstrapAccessible) {
            applyClassloaderDelegation(classLoader.getClass());
            isBootstrapAccessible = areBootstrapClassesAvailable(classLoader);
        }

        bootstrapClassesAccessible.put(classLoader, isBootstrapAccessible);
        return isBootstrapAccessible;
    }

    private void applyClassloaderDelegation(Class<? extends ClassLoader> classLoaderClass) {
        if (classLoadersToInstrument.contains(classLoaderClass)) {
            return; //there was already an attempt made to add delegation to this class
        }
        //first step - make sure that the classloader loading this classloader has access to the bootstrap so that we can apply the instrumentation
        boolean bootstrapAvailableInContainingLoader = makeBootstrapClassesAvailable(classLoaderClass.getClassLoader());
        if (!bootstrapAvailableInContainingLoader) {
            return; //no need to continue as we cannot instrument this classloader
        }

        //mark the classloader for ClassloaderDelegationSensor so that the classloader delegation gets applied
        classLoadersToInstrument.add(classLoaderClass);
        try {
            log.debug("Adding classloader delegation to '{}'", classLoaderClass.getName());
            instrumentation.retransformClasses(classLoaderClass);
        } catch (Throwable t) {
            log.error("Error adding classloader delegation to '{}'", classLoaderClass.getName(), t);
        }
    }

    private boolean areBootstrapClassesAvailable(ClassLoader classLoader) {
        try {
            Class<?> fetched = Class.forName(PROBE_CLASS.getName(), false, classLoader);
            return fetched == PROBE_CLASS;
        } catch (Throwable t) {
            return false;
        }
    }

    @Component
    public class ClassloaderDelegationSensor implements SpecialSensor {

        private final ElementMatcher<MethodDescription> LOAD_CLASS_MATCHER =
                named("loadClass").and(
                        takesArguments(String.class, boolean.class)
                                .or(takesArguments(String.class)));

        @Override
        public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
            return classLoadersToInstrument.contains(clazz);
        }

        @Override
        public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
            return false;
        }

        @Override
        public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
            return builder.visit(Advice.to(ClassloaderDelegationAdvice.class).on(LOAD_CLASS_MATCHER));
        }
    }

    private static class ClassloaderDelegationAdvice {

        @Advice.OnMethodEnter(skipOn = Class.class)
        public static Class<?> enter(@Advice.Argument(value = 0) String classname) {
            //we have to hardcode the classname as the bytecode gets inlined
            if (classname != null && classname.startsWith("rocks.inspectit.oce.bootstrap")) {
                try {
                    //delegate the loading to the bootstrap
                    return Class.forName(classname, false, null);
                } catch (ClassNotFoundException e) {
                    //should never occur as we put our stuff on the bootstrap
                    return null; //execute the classloader normally
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
}
