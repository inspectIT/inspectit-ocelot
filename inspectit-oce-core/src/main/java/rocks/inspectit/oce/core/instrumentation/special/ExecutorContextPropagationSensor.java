package rocks.inspectit.oce.core.instrumentation.special;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.context.ContextManagerImpl;

import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Component
@DependsOn(ContextManagerImpl.BEAN_NAME)
@Slf4j
public class ExecutorContextPropagationSensor implements SpecialSensor {
    @Override
    public boolean shouldInstrument(TypeDescription type, InstrumentationSettings settings) {
        val matcher = isSubTypeOf(Executor.class)
                .and(not(isAbstract()))
                .and(not(nameStartsWith("io.grpc.Context$")
                        .and(
                                nameEndsWith("CurrentContextExecutor").or(nameEndsWith("FixedContextExecutor")))));
        return settings.getSpecial().isExecutorContextPropagation() && matcher.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescription type, InstrumentationSettings first, InstrumentationSettings second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, TypeDescription type, InstrumentationSettings settings, DynamicType.Builder builder) {
        return builder.visit(
                Advice.to(ExecutorAdvice.class)
                        .on(named("execute").and(takesArgument(0, Runnable.class))));
    }

    private static class ExecutorAdvice {
        /**
         * Wraps the given runnable of {@link java.util.concurrent.Executor#execute(Runnable)} via {@link ContextManagerImpl#wrap(Runnable)}}
         *
         * @param runnable
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            runnable = Instances.contextManager.wrap(runnable);
        }
    }
}
