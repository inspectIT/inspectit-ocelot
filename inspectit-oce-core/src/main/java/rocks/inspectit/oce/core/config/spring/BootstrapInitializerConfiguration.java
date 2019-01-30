package rocks.inspectit.oce.core.config.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.noop.NoopContextManager;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.context.ContextManagerImpl;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import javax.annotation.PreDestroy;

/**
 * This configuration creates the beans implementing bootstrap interfaces.
 * The reason for not using {@link org.springframework.stereotype.Component} or {@link org.springframework.stereotype.Service}
 * on such beans is that Spring is unable to resolve their implemented interfaces as they are on the bootstrap.
 * <p>
 * Any class requiring the corresponding bootstrap interface to be initialized should use a {@link org.springframework.context.annotation.DependsOn}
 * annotation referring to this interface.
 */
@Configuration
public class BootstrapInitializerConfiguration {

    @Bean(ContextManagerImpl.BEAN_NAME)
    public ContextManagerImpl getContextManager(@Autowired CommonTagsManager commonTagsManager, @Autowired InstrumentationConfigurationResolver config) {
        ContextManagerImpl contextManager = new ContextManagerImpl(commonTagsManager, config);
        Instances.contextManager = contextManager;
        return contextManager;
    }

    @PreDestroy
    void destroy() {
        Instances.contextManager = NoopContextManager.INSTANCE;
    }
}
