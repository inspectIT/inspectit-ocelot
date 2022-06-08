package rocks.inspectit.ocelot.core;

import io.opencensus.tags.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import rocks.inspectit.ocelot.bootstrap.IAgent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.spring.SpringConfiguration;
import rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.util.Optional;

/**
 * Implementation for the {@link IAgent} interface.
 * This class is responsible for setting up the spring context for inspectIT.
 *
 * @author Jonas Kunz
 */
public class AgentImpl implements IAgent {

    /**
     * Reference to the class loader of the current agent implementation.
     */
    public static final ClassLoader AGENT_CLASS_LOADER = AgentImpl.class.getClassLoader();

    /**
     * Logger that is initialized in the static init block
     */
    private static final Logger LOGGER;

    /**
     * The file used to load the agent's version.
     */
    private static final String AGENT_VERSION_INFORMATION_FILE = "/ocelot-version.info";

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

    /**
     * The version of open telemetry that was used for building the agent.
     */
    private String openTelemetryVersion;

    /**
     * The date the agent was built.
     */
    private String agentBuildDate;

    @Override
    public void start(String cmdArgs, Instrumentation instrumentation) {
        LOGGER.info("Starting inspectIT Ocelot Agent...");
        LOGGER.info("\tVersion: {}", getVersion());
        LOGGER.info("\tBuild Date: {}", getBuildDate());
        logOpenTelemetryClassLoader();

        ctx = new AnnotationConfigApplicationContext();
        ctx.setClassLoader(AGENT_CLASS_LOADER);
        InspectitEnvironment environment = new InspectitEnvironment(ctx, Optional.ofNullable(cmdArgs));

        // once we have the environment, init the logging with the config
        LogbackInitializer.initLogging(environment.getCurrentConfig());

        ctx.registerShutdownHook();

        // add event lister in order to close the inspectit class loader at context closing
        // cast listener so we'll only receive context closed events
        ctx.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> {
            if (AGENT_CLASS_LOADER instanceof URLClassLoader) {
                try {
                    LOGGER.info("Closing inspectIT class loader.");
                    ((URLClassLoader) AGENT_CLASS_LOADER).close();
                } catch (IOException e) {
                    LOGGER.error("Failed closing inspectIT class loader.", e);
                }
            }
        });

        // allows to use autowiring to acquire the Instrumentation instance
        ctx.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("instrumentation", instrumentation));

        ctx.register(SpringConfiguration.class);
        ctx.refresh();
    }

    private void logOpenTelemetryClassLoader() {
        if (Tags.class.getClassLoader() == AgentImpl.class.getClassLoader()) {
            LOGGER.info("OpenTelemetry was loaded in inspectIT classloader");
        } else {
            LOGGER.info("OpenTelemetry was loaded in bootstrap classloader");
        }
    }

    /**
     * Loads the agent's version information from the {@link #AGENT_VERSION_INFORMATION_FILE} file.
     */
    private void readVersionInformation() {
        try (InputStream inputStream = AgentImpl.class.getResourceAsStream(AGENT_VERSION_INFORMATION_FILE)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            agentVersion = reader.readLine();
            openTelemetryVersion = reader.readLine();
            agentBuildDate = reader.readLine();
        } catch (Exception e) {
            LOGGER.warn("Could not read agent version information file.");
            agentVersion = "UNKNOWN";
            agentBuildDate = "UNKNOWN";
        }
    }

    @Override
    public String getVersion() {
        if (agentVersion == null) {
            readVersionInformation();
        }
        return agentVersion;
    }

    @Override
    public String getOpenTelemetryVersion() {
        if (openTelemetryVersion == null) {
            readVersionInformation();
        }
        return openTelemetryVersion;
    }

    @Override
    public String getBuildDate() {
        if (agentBuildDate == null) {
            readVersionInformation();
        }
        return agentBuildDate;
    }

    @Override
    public void destroy() {
        LOGGER.info("Shutting down inspectIT Ocelot Agent");
        ctx.close();
    }
}
