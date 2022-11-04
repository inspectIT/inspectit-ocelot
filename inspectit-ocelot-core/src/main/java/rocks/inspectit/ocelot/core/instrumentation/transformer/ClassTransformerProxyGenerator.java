package rocks.inspectit.ocelot.core.instrumentation.transformer;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible to instantiate an instance of {@link ClassFileTransformer} interface and proxy all calls to
 * the configured {@link  ClassTransformer}.
 * <p>
 * We decided to introduce this indirection to support runtimes <= and >= Java8. Since {@link ClassFileTransformer}
 * has a new method since Java9 to support Jigsaw module system, we created the instance on the fly and will receive
 * an implementation for the current runtime.
 */
@Component
@Slf4j
public class ClassTransformerProxyGenerator {

    @Autowired
    private Instrumentation instrumentation;

    @VisibleForTesting
    @Autowired
    List<ClassTransformer> classTransformers;

    @VisibleForTesting
    ClassTransformer activeTransformer;

    @VisibleForTesting
    ClassFileTransformer transformerProxy;

    @PostConstruct
    public void init() {
        activeTransformer = findActiveTransformer();
        transformerProxy = createAndInstantiateClassTransformerProxy(activeTransformer);
        instrumentation.addTransformer(transformerProxy, true);
        log.info("Proxying ClassFileTransformer to : {}", activeTransformer.getClass().getName());

    }

    @PreDestroy
    public void destroy() {
        if (activeTransformer != null) {
            activeTransformer.destroy();
        }
        instrumentation.removeTransformer(transformerProxy);
    }

    /**
     * Creates ByteBuddy instance of {@link ClassFileTransformer} and delegates all class to our {@link ClassTransformer}
     * implementation
     *
     * @param delegate The {@link ClassTransformer} instance
     *
     * @return The {@link ClassFileTransformer} instance to be registered at the {@link Instrumentation} instance
     *
     * @throws IllegalStateException if {@link ClassFileTransformer} instantiation fails
     */
    @VisibleForTesting
    ClassFileTransformer createAndInstantiateClassTransformerProxy(ClassTransformer delegate) {
        try {
            return new ByteBuddy().subclass(ClassFileTransformer.class)
                    .method(ElementMatchers.named("transform"))
                    .intercept(MethodDelegation.to(delegate))
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded()
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate ClassFileTransformer ByteBuddy Proxy", e);
        }
    }

    /**
     * Determines the actual {@link ClassTransformer} to be used. Only one {@link ClassTransformer} can be active.
     *
     * @return The curren active {@link ClassTransformer}
     *
     * @throws IllegalStateException if zero ore more than one {@link ClassTransformer} are found.
     */
    private ClassTransformer findActiveTransformer() {
        List<ClassTransformer> transformers = classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .collect(Collectors.toList());

        if (transformers.size() != 1) {
            if (transformers.size() == 0) {
                throw new IllegalStateException("No active ClassTransformer found!");
            } else {
                throw new IllegalStateException("Found more than one active ClassTransformer: " + StringUtils.join(transformers, ","));

            }
        }
        return transformers.get(0);
    }

}
