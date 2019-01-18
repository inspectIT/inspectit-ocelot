package rocks.inspectit.oce.core.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.oce.bootstrap.ContextManager;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.core.instrumentation.context.ContextManagerImpl;

import javax.annotation.PreDestroy;

/**
 * This configuration creates the beans implementing bootstrap interfaces.
 * The reason for not using {@link org.springframework.stereotype.Component} or {@link org.springframework.stereotype.Service}
 * on such beans is that Spring is unable to resolve their implemented interfaces as they are on the bootstrap.
 * <p>
 * Any class requiring the corresponding bootstrap interface to be initialized should use a {@link org.springframework.context.annotation.DependsOn}
 * annotation.
 */
@Configuration
public class BootstrapInitializerConfiguration {

    @Bean(ContextManagerImpl.BEAN_NAME)
    ContextManagerImpl getContextManager() {
        ContextManagerImpl contextManager = new ContextManagerImpl();
        Instances.contextManager = contextManager;
        return contextManager;
    }

    @PreDestroy
    void destroy() {
        Instances.contextManager = ContextManager.NOOP;
    }
}
