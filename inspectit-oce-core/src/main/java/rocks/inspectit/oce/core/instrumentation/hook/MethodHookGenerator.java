package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.utils.CommonUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * This class is responsible for translating {@link rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration}s
 * into executable {@link MethodHook}s.
 */
@Component
@Slf4j
public class MethodHookGenerator {

    @Autowired
    private ContextManager contextManager;

    public MethodHook buildHook(Class<?> declaringClass, MethodDescription method, MethodHookConfiguration config) {
        val builder = MethodHook.builder()
                .inspectitContextManager(contextManager)
                .methodName(CommonUtils.getSignature(method))
                .sourceConfiguration(config);

        addReflectionInformationToHook(declaringClass, method, builder);


        builder.entryActions(new CopyOnWriteArrayList<>(Arrays.asList(new IHookAction() {
            @Override
            public void execute(IHookAction.ExecutionContext ctx) {
                log.info("###Entering {}", ctx.getHook().getMethodName());
                ctx.getInspectitContext().getData().forEach(e -> log.info("###   {}={}", e.getKey(), e.getValue()));
            }

            @Override
            public String getName() {
                return "Enter-print";
            }
        })));

        builder.exitActions(new CopyOnWriteArrayList<>(Arrays.asList(new IHookAction() {
            @Override
            public void execute(IHookAction.ExecutionContext ctx) {
                log.info("###exiting {}", ctx.getHook().getMethodName());
                ctx.getInspectitContext().getData().forEach(e -> log.info("###   {}={}", e.getKey(), e.getValue()));
            }

            @Override
            public String getName() {
                return "Exit-print";
            }
        })));

        return builder.build();
    }

    private void addReflectionInformationToHook(Class<?> declaringClass, MethodDescription method, MethodHook.MethodHookBuilder builder) {
        builder.hookedClass(new WeakReference<>(declaringClass));
        builder.hookedConstructor(
                Stream.of(declaringClass.getDeclaredConstructors())
                        .filter(method::represents)
                        .findFirst()
                        .map(c -> new WeakReference<Constructor<?>>(c))
                        .orElse(null));

        builder.hookedMethod(
                Stream.of(declaringClass.getDeclaredMethods())
                        .filter(method::represents)
                        .findFirst()
                        .map(m -> new WeakReference<>(m))
                        .orElse(null));
    }
}
