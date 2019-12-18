package rocks.inspectit.ocelot.core.instrumentation.special.traceinjector;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Special sensor for automatically injecting the trace id into log messages done using the Log4J framework.
 */
@Component
public class Log4JTraceIdAutoInjector implements SpecialSensor {

    /**
     * Targeted classes to instrument.
     */
    private static final ElementMatcher<TypeDescription> CLASSES_MATCHER = is(named("org.apache.log4j.Category")).or(hasSuperType(named("org.apache.log4j.Category")));

    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getTracingSettings().getLogCorrelation().getTraceIdAutoInjection().isEnabled() && CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> clazz, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false;  //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration settings, DynamicType.Builder builder) {
        return builder
                .visit(ForcedLogAdvice.TARGET);
    }

    /**
     * On org.apache.log4j.Category#forcedLog(java.lang.String, org.apache.log4j.Priority, java.lang.Object, java.lang.Throwable)
     * the current trace id will be injected into the log's message attribute.
     */
    private static class ForcedLogAdvice {

        static final AsmVisitorWrapper.ForDeclaredMethods TARGET = Advice.to(ForcedLogAdvice.class).on(named("forcedLog"));

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 2, readOnly = false) Object message) {
            message = Instances.traceIdInjector.injectTraceId(message);
        }
    }
}
