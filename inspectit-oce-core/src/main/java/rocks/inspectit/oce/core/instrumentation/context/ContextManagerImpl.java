package rocks.inspectit.oce.core.instrumentation.context;

import io.grpc.Context;
import rocks.inspectit.oce.bootstrap.ContextManager;
import rocks.inspectit.oce.core.config.spring.BootstrapInitializerConfiguration;

/**
 * Implementation for the bootstrap interface {@link ContextManager}
 */
public class ContextManagerImpl implements ContextManager {

    /**
     * The name of this singleton injected by {@link BootstrapInitializerConfiguration}.
     * Can be used in {@link org.springframework.context.annotation.DependsOn} annotation to ensure correct initialization order.
     */
    public static final String BEAN_NAME = "contextManager";

    @Override
    public Runnable wrap(Runnable r) {
        return Context.current().wrap(r);
    }
}
