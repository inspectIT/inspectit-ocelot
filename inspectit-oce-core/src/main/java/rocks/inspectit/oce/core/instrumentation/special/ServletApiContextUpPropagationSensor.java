package rocks.inspectit.oce.core.instrumentation.special;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.instrumentation.context.ObjectAttachments;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Performs the up-propagation via the Servlet API.
 * This sensor tries to write the response headers as late as possible as long as the response is not committed yet.
 * This is done (a) on the exit of servlet / filter methods or (b) when getOuputStream() getWriter() is called on the ServletResponse.
 * <p>
 * This sensor can lead to the up-propagation being performed multiple times, which however is fine because we want it to happen as late as possible.
 */
@Component
@DependsOn({ContextManager.BEAN_NAME, ObjectAttachments.BEAN_NAME})
public class ServletApiContextUpPropagationSensor implements SpecialSensor {


    private static final ElementMatcher<MethodDescription> SERVLET_OR_FILTER_METHODS =
            named("doFilter").and(isOverriddenFrom(named("javax.servlet.Filter")).and(not(isAbstract())))
                    .or(named("service").and(isOverriddenFrom(named("javax.servlet.Servlet"))).and(not(isAbstract())));

    private static final ElementMatcher<MethodDescription> SERVLET_RESPONSE_COMITTING_METHODS =
            any().and(named("getOutputStream").or(named("getWriter")))
                    .and(isOverriddenFrom(named("javax.servlet.ServletResponse")).and(not(isAbstract())))
                    .and(not(isOverriddenFrom(named("javax.servlet.ServletResponseWrapper"))));


    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isServletApiContextPropagation() &&
                declaresMethod(SERVLET_OR_FILTER_METHODS).or(declaresMethod(SERVLET_RESPONSE_COMITTING_METHODS)).matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        builder = builder.visit(
                Advice.to(ServletOrFilterUpPropagationAdvice.class)
                        .on(SERVLET_OR_FILTER_METHODS));
        builder = builder.visit(
                Advice.to(ServletResponseUpPropagationAdvice.class)
                        .on(SERVLET_RESPONSE_COMITTING_METHODS));
        return builder;
    }


    private static class ServletOrFilterUpPropagationAdvice {

        @Advice.OnMethodEnter
        public static IInspectitContext enter() {
            IInspectitContext ctx = null;
            try {
                ctx = Instances.contextManager.enterNewContext();
                ctx.makeActive();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return ctx;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter IInspectitContext ctx, @Advice.Argument(1) Object response) {
            if (ctx != null) {
                try {
                    Class<?> httpResponseClass = Class.forName("javax.servlet.http.HttpServletResponse", true, response.getClass().getClassLoader());
                    if (httpResponseClass.isInstance(response)) {
                        Method isCommitted = httpResponseClass.getMethod("isCommitted");
                        if (!(Boolean) isCommitted.invoke(response)) {
                            Method setHeader = null;
                            for (val entry : ctx.getUpPropagationHeaders().entrySet()) {
                                if (setHeader == null) {
                                    setHeader = httpResponseClass.getMethod("setHeader", String.class, String.class);
                                }
                                setHeader.invoke(response, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                ctx.close();
            }
        }
    }

    private static class ServletResponseUpPropagationAdvice {

        @Advice.OnMethodEnter
        public static void enter(@Advice.This Object response) {
            try {
                Class<?> httpResponseClass = Class.forName("javax.servlet.http.HttpServletResponse", true, response.getClass().getClassLoader());
                if (httpResponseClass.isInstance(response)) {
                    Method isCommitted = httpResponseClass.getMethod("isCommitted");
                    if (!(Boolean) isCommitted.invoke(response)) {
                        IInspectitContext ctx = Instances.contextManager.enterNewContext();
                        Method setHeader = null;
                        for (val entry : ctx.getUpPropagationHeaders().entrySet()) {
                            if (setHeader == null) {
                                setHeader = httpResponseClass.getMethod("setHeader", String.class, String.class);
                            }
                            setHeader.invoke(response, entry.getKey(), entry.getValue());
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

}
