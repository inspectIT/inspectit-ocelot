package rocks.inspectit.ocelot.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;

import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Component
public class ExecutorContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> EXECUTER_CLASSES_MATCHER = isSubTypeOf(Executor.class);

    private static final ElementMatcher<MethodDescription> EXECUTER_EXECUTE_METHOD_MATCHER =
            named("execute").and(takesArgument(0, Runnable.class));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        val type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isExecutorContextPropagation() &&
                EXECUTER_CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        return builder.visit(
                Advice.to(ExecutorAdvice.class)
                        .on(EXECUTER_EXECUTE_METHOD_MATCHER));
    }

    private static class ExecutorAdvice {
        /**
         * Wraps the given runnable of {@link java.util.concurrent.Executor#execute(Runnable)} via {@link ContextManager#wrap(Runnable)}}
         *
         * @param runnable
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            runnable = Instances.contextManager.wrap(runnable);
        }
    }
}
