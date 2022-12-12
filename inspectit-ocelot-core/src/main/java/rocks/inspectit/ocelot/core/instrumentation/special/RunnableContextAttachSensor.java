package rocks.inspectit.ocelot.core.instrumentation.special;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.ContextTuple;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Advice for restoring a correlation context and doing log-trace correlation when {@link Runnable}s are used. A context
 * is stored in a global map when a Runnable is executed using an {@link java.util.concurrent.ExecutorService}. Once the
 * Runnable is executed, the context stored in the global map, will be restored, thus, spans created during the
 * Runnable execution will be correlated to the trace which was active when the Runnable has been passed to the
 * executor service. Once the Runnable is exiting the context is removed and the previous one is restored.
 */
@Component
public class RunnableContextAttachSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = isSubTypeOf(Runnable.class);

    @Override
    public boolean shouldInstrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings) {
        TypeDescription type = typeWithLoader.getType();

        SpecialSensorSettings sensorSettings = settings.getSource().getSpecial();
        boolean enabled = sensorSettings.isScheduledExecutorContextPropagation() ||
                sensorSettings.isExecutorContextPropagation() ||
                sensorSettings.isThreadStartContextPropagation();

        return enabled && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;  //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder.visit(RunnableRunAdvice.TARGET);
    }

    /**
     * Advice for the {@link Runnable#run()} method.
     */
    private static class RunnableRunAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(RunnableRunAdvice.class).on(named("run").and(takesNoArguments()));

        @Advice.OnMethodEnter
        public static ContextTuple onMethodEnter(@Advice.This Object thiz) {
            return Instances.contextManager.attachContext(thiz);
        }

        @Advice.OnMethodExit
        public static void onMethodExit(@Advice.Enter ContextTuple contextTuple) {
            Instances.contextManager.detachContext(contextTuple);
        }

    }
}
