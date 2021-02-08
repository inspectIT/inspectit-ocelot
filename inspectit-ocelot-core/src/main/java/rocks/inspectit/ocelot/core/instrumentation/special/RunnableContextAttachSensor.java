package rocks.inspectit.ocelot.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.ContextTuple;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Component
public class RunnableContextAttachSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = isSubTypeOf(Runnable.class);

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);

        SpecialSensorSettings sensorSettings = settings.getSource().getSpecial();
        boolean enabled = sensorSettings.isScheduledExecutorContextPropagation() ||
                sensorSettings.isExecutorContextPropagation() ||
                sensorSettings.isThreadStartContextPropagation();

        return enabled && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;  //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder.visit(RunnableRunAdvice.TARGET);
    }

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
