package rocks.inspectit.ocelot.core.instrumentation.special.remote;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IInspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Sensor for reading down-propagated data when receiving a request via the Servlet API.
 */
@Component
public class ServletApiContextDownPropagationSensor implements SpecialSensor {


    private static final ElementMatcher<MethodDescription> DOWN_PROPAGATION_METHODS =
            named("doFilter").and(isOverriddenFrom(named("javax.servlet.Filter")).and(not(isAbstract())))
                    .or(named("service").and(isOverriddenFrom(named("javax.servlet.Servlet"))).and(not(isAbstract())));


    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isServletApiContextPropagation() &&
                declaresMethod(DOWN_PROPAGATION_METHODS).matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        builder = builder.visit(
                Advice.to(DownPropagationAdvice.class)
                        .on(DOWN_PROPAGATION_METHODS));
        return builder;
    }


    private static class DownPropagationAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("unchecked")
        public static IInspectitContext enter(@Advice.Argument(0) Object request) {
            try {
                Class<?> httpRequestClazz = Class.forName("javax.servlet.http.HttpServletRequest", true, request.getClass().getClassLoader());
                if (httpRequestClazz.isInstance(request)) {
                    val ctx = Instances.contextManager.enterNewContext();
                    if (ctx.getData("servlet_api_down_propagation_performed") == null) {
                        ctx.setData("servlet_api_down_propagation_performed", true);

                        Method getHeadersMethod = httpRequestClazz.getMethod("getHeaders", String.class);

                        Map<String, String> headersOfInterest = new HashMap<>();
                        for (String header : ctx.getPropagationHeaderNames()) {
                            val values = (java.util.Enumeration<java.lang.String>) getHeadersMethod.invoke(request, header);
                            if (values != null && values.hasMoreElements()) {
                                headersOfInterest.put(header, String.join(",", Collections.list(values)));
                            }
                        }

                        ctx.readDownPropagationHeaders(headersOfInterest);
                        ctx.makeActive();

                        return ctx;
                    }
                }
            } catch (Throwable t) {
                System.out.println("Error reading propagation data, no data will be propagated: " + t.getMessage());
            }
            return null;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter IInspectitContext ctx) {
            if (ctx != null) {
                ctx.close();
            }
        }

    }

}
