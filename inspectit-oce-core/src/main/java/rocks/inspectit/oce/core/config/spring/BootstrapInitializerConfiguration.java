package rocks.inspectit.oce.core.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopObjectAttachments;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.instrumentation.context.ObjectAttachments;
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

    @Bean(ContextManager.BEAN_NAME)
    public ContextManager getContextManager(CommonTagsManager commonTagsManager, InstrumentationConfigurationResolver config) {
        ContextManager contextManager = new ContextManager(commonTagsManager, config);
        Instances.contextManager = contextManager;
        return contextManager;
    }

    @Bean(ObjectAttachments.BEAN_NAME)
    public ObjectAttachments getObjectAttachments() {
        ObjectAttachments attachments = new ObjectAttachments();
        Instances.attachments = attachments;
        return attachments;
    }

    @PreDestroy
    void destroy() {
        Instances.contextManager = NoopContextManager.INSTANCE;
        Instances.attachments = NoopObjectAttachments.INSTANCE;
    }
}
