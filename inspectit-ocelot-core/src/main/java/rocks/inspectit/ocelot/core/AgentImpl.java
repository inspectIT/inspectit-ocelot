package rocks.inspectit.ocelot.core;

import io.opencensus.tags.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import rocks.inspectit.ocelot.bootstrap.IAgent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.spring.SpringConfiguration;
import rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.util.Optional;

/**
 * Implementation for the {@link IAgent} interface.
 * This class is responsible for setting up the spring context for inspectIT.
 *
 * @author Jonas Kunz
 */
public class AgentImpl implements IAgent {

    /**
     * References the classloader which contains all ocelot classes.
     */
    public static final ClassLoader INSPECTIT_CLASS_LOADER = AgentImpl.class.getClassLoader();

    /**
     * Logger that is initialized in the static init block
     */
    private static final Logger LOGGER;

    /**
     * The file used to load the agent's version.
     */
    private static final String AGENT_VERSION_FILE = "/ocelot-version.info";

    // statically initialize our default logging before doing anything
    static {
        LogbackInitializer.initDefaultLogging();
        LOGGER = LoggerFactory.getLogger(AgentImpl.class);
    }

    /**
     * Created application context.
     */
    private AnnotationConfigApplicationContext ctx;

    /**
     * The agent's version.
     */
    private String agentVersion;

    @Override
    public void start(String cmdArgs, Instrumentation instrumentation) {
        ClassLoader classloader = AgentImpl.class.getClassLoader();

        LOGGER.info("Starting inspectIT Ocelot Agent [version: {}]...", getVersion());
        logOpenCensusClassLoader();

        ctx = new AnnotationConfigApplicationContext();
        ctx.setClassLoader(classloader);
        InspectitEnvironment environment = new InspectitEnvironment(ctx, Optional.ofNullable(cmdArgs));

        // once we have the environment, init the logging with the config
        LogbackInitializer.initLogging(environment.getCurrentConfig());

        ctx.registerShutdownHook();

        //Allows to use autowiring to acquire the Instrumentation instance
        ctx.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("instrumentation", instrumentation));

        ctx.register(SpringConfiguration.class);
        ctx.refresh();
    }

    private void logOpenCensusClassLoader() {
        if (Tags.class.getClassLoader() == AgentImpl.class.getClassLoader()) {
            LOGGER.info("OpenCensus was loaded in inspectIT classloader");
        } else {
            LOGGER.info("OpenCensus was loaded in bootstrap classloader");
        }
    }

    @Override
    public String getVersion() {
        if (agentVersion == null) {
            try {
                InputStream inputStream = AgentImpl.class.getResourceAsStream(AGENT_VERSION_FILE);
                agentVersion = new BufferedReader(new InputStreamReader(inputStream)).readLine();
            } catch (Exception e) {
                LOGGER.warn("Could not read agent version file.");
                agentVersion = "UNKNOWN";
            }
        }
        return agentVersion;
    }

    @Override
    public void destroy() {
        LOGGER.info("Shutting down inspectIT Ocelot Agent");
        ctx.close();
    }
}
