package rocks.inspectit.oce.core.instrumentation.special;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.instrumentation.ClassLoaderDelegationMarker;
import rocks.inspectit.oce.core.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This component performs an instrumentation of {@link ClassLoader}s if.
 * This is required to make our bootstrap classes accessible in custom module systems, such as OSGi.
 * <p>
 * The instrumentation is triggered through the {@link rocks.inspectit.oce.core.instrumentation.InstrumentationTriggerer},
 * which invokes {@link #getClassLoaderClassesRequiringRetransformation(Class, LinkedHashSet)} to find out if any class loaders need tobe instrumented before instrumenting a class.
 * This is currently only used by the {@link rocks.inspectit.oce.core.instrumentation.InstrumentationTriggerer}.
 */
@Component
@Slf4j
public class ClassLoaderDelegation implements SpecialSensor {


    /**
     * Stores which classloaders have already been instrumented.
     * We never deinstrument a classloader after we instrument it, even if {@link SpecialSensorSettings#isClassLoaderDelegation()}
     * is changed to false.
     */
    private final Set<Class<?>> instrumentedClassloaderClasses = Collections.newSetFromMap(
            CacheBuilder.newBuilder().weakKeys().<Class<?>, Boolean>build().asMap());

    private final ElementMatcher<MethodDescription> LOAD_CLASS_MATCHER =
            named("loadClass").and(
                    takesArguments(String.class, boolean.class)
                            .or(takesArguments(String.class)));

    private final ElementMatcher<TypeDescription> CLASS_LOADER_MATCHER =
            isSubTypeOf(ClassLoader.class)
                    .and(not(nameStartsWith("java.")))
                    .and(declaresMethod(LOAD_CLASS_MATCHER));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        boolean isClassLoader = CLASS_LOADER_MATCHER.matches(TypeDescription.ForLoadedType.of(clazz));
        boolean cldEnabled = settings.getSource().getSpecial().isClassLoaderDelegation();
        //we never de instrument a classloader we previously instrumented
        return instrumentedClassloaderClasses.contains(clazz) || (isClassLoader && cldEnabled);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        //remember that this classloader was instrumented
        instrumentedClassloaderClasses.add(clazz);
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

        TypeDescription desc = TypeDescription.ForLoadedType.of(classLoaderClass);
        if (!instrumentedClassloaderClasses.contains(classLoaderClass) && CLASS_LOADER_MATCHER.matches(desc)) {
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
            if (classname != null && classname.startsWith("rocks.inspectit.oce.bootstrap")) {
                //some JVMs, such as IBM implement the bootstrap classloader as a non-native class
                //in this case we need to make sure that the classloader delegation is only performed once
                //because otherwise we end up with a StackOverflowError (the instrument bootstrap loaded would call
                //Class.forName in an infinite recursion)
                boolean cldPerformed = ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.get() != null;
                if (!cldPerformed) {
                    ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.set(true);
                    try {
                        //delegate the loading to the bootstrap
                        return Class.forName(classname, false, null);
                    } catch (ClassNotFoundException e) {
                        //should never occur as we put our stuff on the bootstrap
                        return null; //execute the classloader normally
                    } finally {
                        ClassLoaderDelegationMarker.CLASS_LOADER_DELEGATION_PERFORMED.remove();
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
}
