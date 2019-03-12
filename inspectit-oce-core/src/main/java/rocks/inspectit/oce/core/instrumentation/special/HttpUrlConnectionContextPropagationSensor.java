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
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.instrumentation.context.ObjectAttachments;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Performs up and down propagation for {@link HttpURLConnection} client.
 */
@Component
@DependsOn({ContextManager.BEAN_NAME, ObjectAttachments.BEAN_NAME})
public class HttpUrlConnectionContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<TypeDescription> HTTP_URL_CONNECTION_CLASSES_MATCHER = isSubTypeOf(HttpURLConnection.class);

    /**
     * We down propagate when the first of these three methods is executed
     */
    private static final ElementMatcher<MethodDescription> DOWN_PROPAGATION_TRIGGERING_METHODS =
            named("getInputStream").and(takesArguments(0)).or(
                    named("getOutputStream").and(takesArguments(0))).or(
                    named("connect").and(takesArguments(0)));

    /**
     * This method triggers the up-propagation.
     * It is internally called by other reading methods, such as {@link HttpURLConnection#getResponseCode()}
     */
    private static final ElementMatcher<MethodDescription> UP_PROPAGATION_TRIGGERING_METHODS =
            named("getInputStream").and(takesArguments(0));


    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isHttpUrlConnectionContextPropagation() &&
                HTTP_URL_CONNECTION_CLASSES_MATCHER.matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        builder = builder.visit(
                Advice.to(DownPropagationAdvice.class)
                        .on(DOWN_PROPAGATION_TRIGGERING_METHODS));
        builder = builder.visit(
                Advice.to(UpPropagationAdvice.class)
                        .on(UP_PROPAGATION_TRIGGERING_METHODS));
        return builder;
    }

    private static class DownPropagationAdvice {

        @Advice.OnMethodEnter
        public static void enter(@Advice.This HttpURLConnection thiz) {
            boolean downPropagationPerformed = null != Instances.attachments.getAttachment(thiz, "down_propagation_performed");
            if (!downPropagationPerformed) {
                Instances.attachments.attach(thiz, "down_propagation_performed", true);
                val ctx = Instances.contextManager.enterNewContext();
                ctx.makeActive();
                ctx.getDownPropagationHeaders().forEach(thiz::addRequestProperty);
                ctx.close();
            }
        }

    }


    private static class UpPropagationAdvice {

        @Advice.OnMethodExit
        public static void exit(@Advice.This HttpURLConnection thiz) {
            boolean upPropagationPerformed = null != Instances.attachments.getAttachment(thiz, "up_propagation_performed");
            if (!upPropagationPerformed) {
                Instances.attachments.attach(thiz, "up_propagation_performed", true);
                val ctx = Instances.contextManager.enterNewContext();
                ctx.makeActive();
                Map<String, String> headersOfInterest = new HashMap<>();
                for (String headerName : ctx.getPropagationHeaderFields()) {
                    List<String> values = thiz.getHeaderFields().get(headerName);
                    if (values != null) {
                        headersOfInterest.put(headerName, String.join(",", values));
                    }
                }
                ctx.readPropagationHeaders(headersOfInterest);
                ctx.close();
            }
        }

    }

}
