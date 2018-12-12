package rocks.inspectit.oce.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import rocks.inspectit.oce.bootstrap.IAgent;
import rocks.inspectit.oce.core.rocks.inspectit.oce.core.config.ConfigurationCenter;
import rocks.inspectit.oce.core.rocks.inspectit.oce.core.config.PropertySourcesInitializer;
import rocks.inspectit.oce.core.rocks.inspectit.oce.core.config.SpringConfiguration;

import java.lang.instrument.Instrumentation;

/**
 * Implementation for the {@link IAgent} interface.
 * This clas sis responsible forsetting up the spring context for inspectIT.
 *
 * @author Jonas Kunz
 */
@Slf4j
public class AgentImpl implements IAgent {

    private AnnotationConfigApplicationContext ctx;

    @Override
    public void start(String cmdArgs, Instrumentation instrumentation) {

        log.info("Starting Agent...");
        ctx = new AnnotationConfigApplicationContext();
        ctx.setClassLoader(AgentImpl.class.getClassLoader());
        ctx.registerShutdownHook();

        //Allows to use autowiring to acquire the Instrumentation instance
        ctx.getBeanFactory().registerSingleton("instrumentation", instrumentation);

        PropertySourcesInitializer.configurePropertySources(ctx, cmdArgs);

        ctx.register(SpringConfiguration.class);
        ctx.refresh();
    }


    @Override
    public void destroy() {
        log.info("Shutting down Agent");
        ctx.close();
    }
}
