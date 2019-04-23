package rocks.inspectit.ocelot.core.instrumentation.special.remote;

import lombok.val;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Performs up- and down propagation via the protected doExecute method of the apache CloseableHttpClient.
 */
@Component
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
                System.out.println("Error performing propagation, no data will be propagated: " + t.getMessage());
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
                Class<?> headerClass = Class.forName("org.apache.http.Header", true, httpMessage.getClassLoader());
                Method mHeaderGetValue = headerClass.getMethod("getValue");
                Map<String, String> upPropagationHeaders = new HashMap<>();
                for (String headerName : ctx.getPropagationHeaderNames()) {
                    Object[] headersArray = (Object[]) getHeaders.invoke(response, headerName);

                    StringBuilder result = new StringBuilder();
                    if (headersArray != null) {
                        for (Object header : headersArray) {
                            String value = (String) mHeaderGetValue.invoke(header);
                            if (value != null) {
                                if (result.length() > 0) {
                                    result.append(',');
                                }
                                result.append(value);
                            }
                        }
                    }
                    if (result.length() > 0) {
                        upPropagationHeaders.put(headerName, result.toString());
                    }
                }
                if (!upPropagationHeaders.isEmpty()) {
                    ctx.makeActive();
                    ctx.readUpPropagationHeaders(upPropagationHeaders);
                    ctx.close();
                }

            } catch (
                    Throwable t) {
                System.out.println("Error reading propagation data, no data will be propagated: " + t.getMessage());
            }
        }

    }


}
