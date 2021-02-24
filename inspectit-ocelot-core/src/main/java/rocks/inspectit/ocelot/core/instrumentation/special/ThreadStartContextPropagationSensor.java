package rocks.inspectit.ocelot.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for passing the sensor to new threads. This sensor will pass the context when directly using the
 * {@link Thread} class or classes extending it.
 * <p>
 * When storing the context information, we check whether the thread is started by an executor. This is done using the
 * method {@link IContextManager#insideCorrelation()}. In case this flag is set, we don't store the current context.
 * This has been done because executor services might spawn new worker threads. In case we don't prevent the correlation
 * in this case, the executor's worker thread will receive the current context. This leads to the problem, that each
 * executor task - with has no context - will be related to the trace context attached to the thread.
 */
@Component
public class ThreadStartContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = is(Thread.class).or(isSubTypeOf(Thread.class));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
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
        public static void onMethodEnter(@Advice.This Thread thiz) {
            if (!Instances.contextManager.insideCorrelation()) {
                Instances.contextManager.storeContext(thiz, true);
            }
        }

    }

    /**
     * On {@link Thread#run()} which executes the target, a context related to the given thread, will be restored if available.
     */
    private static class ThreadRunAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ThreadRunAdvice.class).on(named("run"));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Thread thiz) {
            if (Thread.currentThread() == thiz) {
                // We don't have to remove the context from the thread once finished because a thread cannot be started
                // twice, thus, we don't care about the context-tuple
                Instances.contextManager.attachContext(thiz);
            }
        }
    }
}
