package rocks.inspectit.ocelot.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for passing the context via the {@link ExecutorService}.
 * Please read the detailed documentation of {@link ScheduledExecutorContextPropagationSensor} for more information!
 */
@Component
public class ExecutorContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> EXECUTER_CLASSES_MATCHER = isSubTypeOf(Executor.class);

    @Override
    public boolean shouldInstrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings) {
        TypeDescription type = typeWithLoader.getType();
        return settings.getSource().getSpecial().isExecutorContextPropagation() &&
                EXECUTER_CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        return builder.visit(ExecutorAdvice.TARGET);
    }

    /**
     * Advice for the {@link java.util.concurrent.ExecutorService#execute(Runnable)} method.
     */
    private static class ExecutorAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ExecutorAdvice.class)
                .on(named("execute").and(takesArgument(0, Runnable.class)));

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (Instances.contextManager.enterCorrelation()) {
                if (runnable.getClass().getName().contains("$$Lambda$")) {
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
}
