package rocks.inspectit.ocelot.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for passing the sensor to new threads. This sensor will pass the context when directly using the {@link Thread} class or classes extending it.
 */
@Component
public class ThreadStartContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = is(Thread.class).or(isSubTypeOf(Thread.class));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        val type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isThreadStartContextPropagation() && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;  //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder
                .visit(ThreadStartAdvice.TARGET)
                .visit(ThreadRunAdvice.TARGET);
    }

    /**
     * On {@link Thread#start()} the current context will be stored in a global cache.
     */
    private static class ThreadStartAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ThreadStartAdvice.class).on(named("start"));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Thread thread) {
            Instances.contextManager.storeContextForThread(thread);
        }

    }

    /**
     * On {@link Thread#run()} which executes the target, a context related to the given thread, will be restored if available.
     */
    private static class ThreadRunAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ThreadRunAdvice.class).on(named("run"));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Thread thread) {
            if (Thread.currentThread() == thread) {
                Instances.contextManager.attachContextToThread(thread);
                Instances.logTraceCorrelator.applyCorrelationToMDC();
            }
        }

    }
}
