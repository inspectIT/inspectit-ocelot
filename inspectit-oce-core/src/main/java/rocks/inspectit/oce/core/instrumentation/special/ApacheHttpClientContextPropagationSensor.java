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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Performs up-and down propagation via the protected doExecute method of the apache CLoseableHttpClient.
 */
@Component
@DependsOn({ContextManager.BEAN_NAME, ObjectAttachments.BEAN_NAME})
public class ApacheHttpClientContextPropagationSensor implements SpecialSensor {

    private static final ElementMatcher<MethodDescription> DO_EXECUTE_METHOD =
            named("doExecute")
                    .and(isOverriddenFrom(named("org.apache.http.impl.client.CloseableHttpClient")))
                    .and(not(isAbstract()));


    @Override
    public boolean shouldInstrument(Class<?> clazz, InstrumentationConfiguration settings) {
        TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
        return settings.getSource().getSpecial().isApacheHttpClientContextPropagation() &&
                declaresMethod(DO_EXECUTE_METHOD).matches(type);
    }

    @Override
    public boolean requiresInstrumentationChange(Class<?> type, InstrumentationConfiguration first, InstrumentationConfiguration second) {
        return false; //if the sensor stays active it never requires changes
    }

    @Override
    public DynamicType.Builder instrument(Class<?> clazz, InstrumentationConfiguration conf, DynamicType.Builder builder) {
        builder = builder.visit(
                Advice.to(PropagationAdvice.class)
                        .on(DO_EXECUTE_METHOD));

        return builder;
    }

    private static class PropagationAdvice {

        @Advice.OnMethodEnter
        public static Map<String, String> enter(@Advice.Argument(1) Object request) {
            try {
                val ctx = Instances.contextManager.enterNewContext();
                ctx.makeActive();

                Class<?> httpMessage = Class.forName("org.apache.http.HttpMessage", true, request.getClass().getClassLoader());
                Method setHeader = httpMessage.getMethod("setHeader", String.class, String.class);

                Map<String, String> downPropagationHeaders = ctx.getDownPropagationHeaders();
                for (val entry : downPropagationHeaders.entrySet()) {
                    setHeader.invoke(request, entry.getKey(), entry.getValue());
                }
                ctx.close();
                return downPropagationHeaders;

            } catch (Throwable t) {
                t.printStackTrace();
            }
            return null;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter Map<String, String> setHeaders, @Advice.Argument(1) Object request, @Advice.Return Object response) {
            try {
                Class<?> httpMessage = Class.forName("org.apache.http.HttpMessage", true, request.getClass().getClassLoader());
                //clean up the down propagation headers set so that they are not present any more in the message
                if (setHeaders != null && !setHeaders.isEmpty()) {
                    Method removeHeaders = httpMessage.getMethod("removeHeaders", String.class);
                    for (String header : setHeaders.keySet()) {
                        removeHeaders.invoke(request, header);
                    }
                }
                //perform up-propagation
                val ctx = Instances.contextManager.enterNewContext();

                Method getHeaders = httpMessage.getMethod("getHeaders", String.class);
                Method headerGetValue = null;
                Map<String, String> upPropagationHeaders = new HashMap<>();
                for (String headerName : ctx.getPropagationHeaderFields()) {
                    Object headersArray = getHeaders.invoke(response, headerName);
                    int numHeaders = Array.getLength(headersArray);
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < numHeaders; i++) {
                        Object header = Array.get(headersArray, i);
                        if (headerGetValue == null) {
                            Class<?> headerClass = Class.forName("org.apache.http.Header", true, header.getClass().getClassLoader());
                            headerGetValue = headerClass.getMethod("getValue");
                        }
                        if (result.length() > 0) {
                            result.append(',');
                        }
                        result.append(headerGetValue.invoke(header));
                    }

                    if (result.length() > 0) {
                        upPropagationHeaders.put(headerName, result.toString());
                    }
                }
                if (!upPropagationHeaders.isEmpty()) {
                    ctx.makeActive();
                    ctx.readPropagationHeaders(upPropagationHeaders);
                    ctx.close();
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }


}
