package rocks.inspectit.ocelot.core.instrumentation.special.traceinjector;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for automatically injecting the trace id into log messages done using the Log4J2 framework.
 */
@Component
public class Log4J2TraceIdAutoInjector implements SpecialSensor {

    /**
     * Targeted classes to instrument.
     */
    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = hasSuperType(named("org.apache.logging.log4j.message.MessageFactory"));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getTracingSettings()
                .getLogCorrelation()
                .getTraceIdAutoInjection()
                .isEnabled() && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;  //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder.visit(NewMessageCharSequenceAdvice.TARGET).visit(NewMessageObjectAdvice.TARGET);
    }

    /**
     * On newMessage(java.lang.String) and newMessage(java.lang.CharSequence) of org.apache.logging.log4j.message.AbstractMessageFactory
     * the current trace id will be injected into the log's message attribute.
     */
    private static class NewMessageCharSequenceAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(NewMessageCharSequenceAdvice.class)
                .on(named("newMessage").and(takesArgument(0, String.class).or(takesArgument(0, CharSequence.class))));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false, typing = Assigner.Typing.DYNAMIC) CharSequence message) {
            message = (String) Instances.traceIdInjector.injectTraceId(message);
        }
    }

    /**
     * On org.apache.logging.log4j.message.AbstractMessageFactory#newMessage(java.lang.Object)
     * the current trace id will be injected into the log's message attribute.
     */
    private static class NewMessageObjectAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(NewMessageObjectAdvice.class)
                .on(named("newMessage").and(takesArgument(0, Object.class)));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Object message) {
            message = Instances.traceIdInjector.injectTraceId(message);
        }
    }
}
