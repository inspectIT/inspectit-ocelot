package rocks.inspectit.ocelot.core.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.noop.NoopContextManager;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopTraceIdInjector;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.*;
import rocks.inspectit.ocelot.bootstrap.opentelemetry.NoopOpenTelemetryController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.AgentInfoImpl;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.actions.InspectitWrapperImpl;
import rocks.inspectit.ocelot.core.instrumentation.actions.cache.InspectitReflectionImpl;
import rocks.inspectit.ocelot.core.instrumentation.actions.cache.InspectitRegexImpl;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.context.ObjectAttachmentsImpl;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.LogTraceCorrelatorImpl;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.MdcAccessManager;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl;
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
    public ContextManager getContextManager(CommonTagsManager commonTagsManager, PropagationSessionStorage sessionStorage, InstrumentationConfigurationResolver config) {
        ContextManager contextManager = new ContextManager(commonTagsManager, sessionStorage, config);
        Instances.contextManager = contextManager;
        return contextManager;
    }

    @Bean(ObjectAttachmentsImpl.BEAN_NAME)
    public ObjectAttachmentsImpl getObjectAttachments() {
        ObjectAttachmentsImpl attachments = new ObjectAttachmentsImpl();
        Instances.attachments = attachments;
        return attachments;
    }

    @Bean(AgentInfoImpl.BEAN_NAME)
    public AgentInfoImpl getInspectitAgentInfo() {
        String version = AgentManager.getAgentVersion();
        AgentInfoImpl agentInfo = new AgentInfoImpl(version);
        Instances.agentInfo = agentInfo;
        return agentInfo;
    }

    @Bean(InspectitReflectionImpl.BEAN_NAME)
    public InspectitReflectionImpl getInspectitReflection() {
        InspectitReflectionImpl reflection = new InspectitReflectionImpl();
        Instances.reflection = reflection;
        return reflection;
    }

    @Bean(InspectitRegexImpl.BEAN_NAME)
    public InspectitRegexImpl getInspectitRegex() {
        InspectitRegexImpl regex = new InspectitRegexImpl();
        Instances.regex = regex;
        return regex;
    }

    @Bean(InspectitWrapperImpl.BEAN_NAME)
    public InspectitWrapperImpl getInspectitWrapper() {
        InspectitWrapperImpl wrapper = new InspectitWrapperImpl();
        Instances.wrapper = wrapper;
        return wrapper;
    }

    @Bean(LogTraceCorrelatorImpl.BEAN_NAME)
    public LogTraceCorrelatorImpl getLogTraceCorrelator(MdcAccessManager mdcAccessManager, InspectitEnvironment environment) {
        InspectitConfig configuration = environment.getCurrentConfig();
        String traceIdKey = configuration.getTracing().getLogCorrelation().getTraceIdMdcInjection().getKey();

        return new LogTraceCorrelatorImpl(mdcAccessManager, traceIdKey);
    }

    @Bean(OpenTelemetryControllerImpl.BEAN_NAME)
    public OpenTelemetryControllerImpl getOpenTelemetryController() {
        return new OpenTelemetryControllerImpl();
    }

    @PreDestroy
    void destroy() {
        Instances.contextManager = NoopContextManager.INSTANCE;
        Instances.attachments = NoopObjectAttachments.INSTANCE;
        Instances.agentInfo = NoopInspectitAgentInfo.INSTANCE;
        Instances.reflection = NoopInspectitReflection.INSTANCE;
        Instances.regex = NoopInspectitRegex.INSTANCE;
        Instances.wrapper = NoopInspectitWrapper.INSTANCE;
        Instances.hookManager = NoopHookManager.INSTANCE;
        Instances.logTraceCorrelator = NoopLogTraceCorrelator.INSTANCE;
        Instances.traceIdInjector = NoopTraceIdInjector.INSTANCE;
        Instances.openTelemetryController = NoopOpenTelemetryController.INSTANCE;
    }
}
