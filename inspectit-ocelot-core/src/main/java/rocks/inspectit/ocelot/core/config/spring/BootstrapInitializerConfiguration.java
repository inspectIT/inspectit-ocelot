package rocks.inspectit.ocelot.core.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopObjectAttachments;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.context.ObjectAttachmentsImpl;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.LogTraceCorrelatorImpl;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MDCAccess;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

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

    @Bean(ObjectAttachmentsImpl.BEAN_NAME)
    public ObjectAttachmentsImpl getObjectAttachments() {
        ObjectAttachmentsImpl attachments = new ObjectAttachmentsImpl();
        Instances.attachments = attachments;
        return attachments;
    }

    @Bean(LogTraceCorrelatorImpl.BEAN_NAME)
    public LogTraceCorrelatorImpl getLogTraceCorrelator(MDCAccess access, InspectitEnvironment env) {
        String key = env.getCurrentConfig().getTracing().getLogCorrelation().getKey();
        LogTraceCorrelatorImpl correlator = new LogTraceCorrelatorImpl(access, key);
        return correlator;
    }

    @PreDestroy
    void destroy() {
        Instances.contextManager = NoopContextManager.INSTANCE;
        Instances.attachments = NoopObjectAttachments.INSTANCE;
    }
}
