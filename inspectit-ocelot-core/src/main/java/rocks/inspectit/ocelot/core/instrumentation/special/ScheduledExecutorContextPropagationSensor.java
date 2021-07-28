package rocks.inspectit.ocelot.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IContextManager;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for passing the context via the {@link ScheduledExecutorService}.
 * This sensor will pass the context when using the following methods:
 * <p><ul>
 * <li>{@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
 * <li>{@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
 * </ul>
 * <p>
 * In the advices which are injected into the executor methods, the following is done:
 * <p>
 * The advice will wrap the {@link java.lang.Runnable} or {@link java.util.concurrent.Callable} for attaching and
 * detaching the current context and doing log-trace correlation. See also {@link io.grpc.Context#wrap(Runnable)}. This
 * is only done in case the Runnable is a lambda class.
 * <p>
 * If the Runnable's class is a named or anonymous class, the current context is stored in a global cache related to
 * the Runnable. See also {@link rocks.inspectit.ocelot.bootstrap.context.IContextManager#storeContext(Object, boolean)}.
 * This is done to prevent any errors when the application requires the Runnable to be in its original class, which
 * is not given if we wrap it. An example for this would be JBoss custom executor which casts the executed Runnable
 * into a known class.
 * <p>
 * In order to prevent unnecessary context passing (e.g. in case multiple executors are being called via delegation
 * executors ({@link java.util.concurrent.Executors.FinalizableDelegatedExecutorService})), the method
 * {@link IContextManager#enterCorrelation()} is called. This is done to set a flag on the current thread in order to
 * mark that a correlation has been done (wrapping or storing the context) and following executors should not doing
 * a correlation as well. This flag is cleaned once the executor's method finishes.
 */

@Component
public class ScheduledExecutorContextPropagationSensor implements SpecialSensor {

    private static final String LAMBDA = "$$Lambda$";

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = isSubTypeOf(ScheduledExecutorService.class);

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        val type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isScheduledExecutorContextPropagation() && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder
                .visit(ScheduledExecutorRunnableAdvice.TARGET)
                .visit(ScheduledExecutorRunnableContinuousAdvice.TARGET)
                .visit(ScheduledExecutorCallableAdvice.TARGET);
    }

    /**
     * Advice for the {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)} method.
     */
    private static class ScheduledExecutorRunnableAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ScheduledExecutorRunnableAdvice.class)
                .on(named("schedule").and(takesArgument(0, Runnable.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (Instances.contextManager.enterCorrelation()) {
                if (runnable.getClass().getName().contains(LAMBDA)) {
                    // order is important because the log-correlator requires the restored context, thus, have to be
                    // called after the context wrapper (needs to be nested by it)
                    runnable = Instances.logTraceCorrelator.wrap(runnable);
                    runnable = Instances.contextManager.wrap(runnable);
                } else {
                    Instances.contextManager.storeContext(runnable, true);
                }
            }
        }

        @Advice.OnMethodExit
        public static void onMethodExit() {
            Instances.contextManager.exitCorrelation();
        }
    }

    /**
     * Advice for the {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} and
     * {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} methods.
     */
    private static class ScheduledExecutorRunnableContinuousAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ScheduledExecutorRunnableContinuousAdvice.class)
                .on(named("scheduleWithFixedDelay").or(named("scheduleAtFixedRate")).and(takesArgument(0, Runnable.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (Instances.contextManager.enterCorrelation()) {
                if (runnable.getClass().getName().contains(LAMBDA)) {
                    // order is important because the log-correlator requires the restored context, thus, have to be
                    // called after the context wrapper (needs to be nested by it)
                    runnable = Instances.logTraceCorrelator.wrap(runnable);
                    runnable = Instances.contextManager.wrap(runnable);
                } else {
                    Instances.contextManager.storeContext(runnable, false);
                }
            }
        }

        @Advice.OnMethodExit
        public static void onMethodExit() {
            Instances.contextManager.exitCorrelation();
        }
    }

    /**
     * Advice for the {@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)} method.
     */
    private static class ScheduledExecutorCallableAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ScheduledExecutorCallableAdvice.class)
                .on(named("schedule").and(takesArgument(0, Callable.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Callable callable) {
            if (Instances.contextManager.enterCorrelation()) {
                if (callable.getClass().getName().contains(LAMBDA)) {
                    // order is important because the log-correlator requires the restored context, thus, have to be
                    // called after the context wrapper (needs to be nested by it)
                    callable = Instances.logTraceCorrelator.wrap(callable);
                    callable = Instances.contextManager.wrap(callable);
                } else {
                    Instances.contextManager.storeContext(callable, true);
                }
            }
        }

        @Advice.OnMethodExit
        public static void onMethodExit() {
            Instances.contextManager.exitCorrelation();
        }
    }
}
