package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.context.ContextManagerImpl;

import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Component
@DependsOn(ContextManagerImpl.BEAN_NAME)
public class ExecutorContextPropagationSensor implements SpecialSensor {

    @Override
    public boolean isEnabledForConfig(InstrumentationSettings conf) {
        return conf.getSpecial().isExecutorContextPropagation();
    }

    @Override
    public AgentBuilder instrument(InstrumentationSettings conf, AgentBuilder agent) {
        return agent.type(isSubTypeOf(Executor.class)
                .and(not(isAbstract()))
                .and(not(nameStartsWith("io.grpc.Context$")
                        .and(
                                nameEndsWith("CurrentContextExecutor").or(nameEndsWith("FixedContextExecutor"))))))
                .transform((builder, type, cl, module) ->
                        builder.visit(
                                Advice.to(ExecutorAdvice.class)
                                        .on(named("execute").and(takesArgument(0, Runnable.class))))
                );
    }

    private static class ExecutorAdvice {
        /**
         * Wraps the given runnable of {@link java.util.concurrent.Executor#execute(Runnable)} via {@link BootstrapInitializer#wrap(Runnable)}
         *
         * @param runnable
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            runnable = Instances.contextManager.wrap(runnable);
        }
    }
}
