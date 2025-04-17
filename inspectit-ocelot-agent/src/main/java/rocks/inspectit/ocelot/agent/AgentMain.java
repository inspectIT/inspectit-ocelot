package rocks.inspectit.ocelot.agent;

import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.AgentProperties;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.jar.JarFile;

/**
 * Entrypoint of the agent.
 *
 * @author Jonas Kunz
 */
public class AgentMain {

    private static final String PUBLISH_OPEN_TELEMETRY_TO_BOOTSTRAP_PROPERTY = "inspectit.publishOpenTelemetryToBootstrap";

    private static final String ASYNC_INSTRUMENTATION_PROPERTY = "inspectit.instrumentation.internal.async";

    private static final String ASYNC_INSTRUMENTATION_ENV_PROPERTY = "INSPECTIT_INSTRUMENTATION_INTERNAL_ASYNC";

    /**
     * Main method for attaching the agent itself to a running JVM.
     *
     * @param args the pid of a JVM
     */
    public static void main(String[] args) {
        boolean error = (args.length != 1 && args.length != 2) || !args[0].matches("\\d+");

        if (error) {
            System.err.println("Please specify the PID of the JVM you want the agent attach to.\nNote: you can pass properties to the agent represented as a JSON string!\n\nUsage: <PID> [AGENT_PROPERTIES]");
            System.exit(1);
        }

        String agentProperties = args.length == 2 ? args[1] : null;
        AgentAttacher.attach(Integer.parseInt(args[0]), agentProperties);
    }

    // invoked if agent is started after the instrumented application
    public static void agentmain(String agentArgs, Instrumentation inst) {
        //TODO: currently replacing the agent does not really work as all Agent versions share the same namespace in the same classpath
        if (!isAsyncInstrumentationEnabled()) {
            System.out.println(ASYNC_INSTRUMENTATION_PROPERTY + " and " + ASYNC_INSTRUMENTATION_ENV_PROPERTY + " are ignored when when attaching agent to a running application!");
        }
        startAgent(agentArgs, inst, true);
    }

    // invoked if agent is started before the instrumented application
    public static void premain(String agentArgs, Instrumentation inst) {
        boolean loadOpenTelemetryJarToBootstrap = "true".equalsIgnoreCase(System.getProperty(PUBLISH_OPEN_TELEMETRY_TO_BOOTSTRAP_PROPERTY));

        try {
            if (loadOpenTelemetryJarToBootstrap) {
                Path otelJar = AgentJars.getOpenTelemetryJar();
                inst.appendToBootstrapClassLoaderSearch(new JarFile(otelJar.toFile()));
            }

            if (isAsyncInstrumentationEnabled()) {
                // we make sure that the startup of inspectIT is asynchronous
                new Thread(() -> startAgent(agentArgs, inst, !loadOpenTelemetryJarToBootstrap)).start();
            } else {
                String javaVersion = System.getProperty("java.version");
                if (javaVersion.startsWith("1.8")) {
                    System.err.println("Asynchronous instrumentation is disabled in a pre Java 9 environment! This results in a significant boot time performance degradation! See: https://bugs.openjdk.java.net/browse/JDK-7018422");
                }
                startAgent(agentArgs, inst, !loadOpenTelemetryJarToBootstrap);
            }
        } catch (Exception e) {
            System.err.println("Error starting inspectIT Agent!");
            e.printStackTrace();
        }
    }

    private static void startAgent(String agentArgs, Instrumentation inst, boolean includeOpenTelemetryInInspectitLoader) {
        delayAgentStart();

        try {
            InspectITClassLoader icl = initializeInspectitLoader(inst, includeOpenTelemetryInInspectitLoader);
            AgentManager.startOrReplaceInspectitCore(icl, agentArgs, inst);
        } catch (Exception e) {
            System.err.println("Error starting inspectIT Agent!");
            e.printStackTrace();
        }
    }

    private static void delayAgentStart() {
        String startDelayViaSystemProperty = System.getProperty(AgentProperties.START_DELAY_PROPERTY);
        String startDelayViaEnvironmentVariable = System.getenv(AgentProperties.START_DELAY_ENV_PROPERTY);
        if (startDelayViaSystemProperty != null) {
            delayAgentStart(startDelayViaSystemProperty, "Value '%s' of system property " + AgentProperties.START_DELAY_PROPERTY + " does not contain a positive integer value. Continuing without delay.");
        } else if (startDelayViaEnvironmentVariable != null) {
            delayAgentStart(startDelayViaEnvironmentVariable, "Value '%s' of environment variable " + AgentProperties.START_DELAY_ENV_PROPERTY + " does not contain a positive integer value. Continuing without delay.");
        }
    }

    private static void delayAgentStart(String startDelay, String errorMessageFormat) {
        try {
            int delayInMilliseconds = Integer.parseInt(startDelay);
            if (delayInMilliseconds > 0) {
                System.out.println("Delaying start of InspectIT for " + delayInMilliseconds + " ms.");
                Thread.sleep(delayInMilliseconds);
            } else {
                System.err.printf(errorMessageFormat + "%n", startDelay);
            }
        } catch (InterruptedException ie) {
            System.err.println("Interrupted while delaying initialization");
        } catch (NumberFormatException nfe) {
            System.err.printf(errorMessageFormat + "%n", startDelay);
        }
    }

    private static boolean isAsyncInstrumentationEnabled() {
        String isAsyncInstrumentationPropertyValue = null != System.getProperty(ASYNC_INSTRUMENTATION_PROPERTY) ? System.getProperty(ASYNC_INSTRUMENTATION_PROPERTY) : System.getenv(ASYNC_INSTRUMENTATION_ENV_PROPERTY);
        return null == isAsyncInstrumentationPropertyValue || "true".equalsIgnoreCase(isAsyncInstrumentationPropertyValue);
    }

    /**
     * Loads inspectit-ocelot-bootstrap with the bootstrap classloader and inspectit-ocelot-core with a new inspectIT loader.
     *
     * @return the created inspectIT classloader
     */
    private static InspectITClassLoader initializeInspectitLoader(Instrumentation inst, boolean includeOpenTelemetry) throws IOException {
        Path bootstrapJar = AgentJars.getOcelotBootstrapJar();
        inst.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar.toFile()));

        Instances.BOOTSTRAP_JAR_URL = bootstrapJar.toUri().toURL();

        Instances.AGENT_JAR_URL = AgentMain.class.getProtectionDomain().getCodeSource().getLocation();

        Path coreJar = AgentJars.getOcelotCoreJar();
        InspectITClassLoader icl = new InspectITClassLoader(new URL[]{coreJar.toUri().toURL()});

        if (includeOpenTelemetry) {
            Path otelJar = AgentJars.getOpenTelemetryJar();
            icl.addURL(otelJar.toUri().toURL());
        }

        return icl;
    }

    /**
     * private inspectIT classloader to ensure our classes are hidden from other application classes
     */
    public static class InspectITClassLoader extends URLClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }

        InspectITClassLoader(URL[] urls) {
            super(urls, findParentClassLoader());
        }

        /**
         * Visibility changed to public.
         */
        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected PermissionCollection getPermissions(CodeSource codesource) {
            // apply the all permission policy to all of our classes and packages.
            AllPermission allPerm = new AllPermission();
            PermissionCollection pc = allPerm.newPermissionCollection();
            pc.add(allPerm);
            return pc;
        }

        /**
         * @return Returns the platform class loader if we are on Java 9 as this one can load needed
         * Java classes for us.
         */
        private static ClassLoader findParentClassLoader() {
            try {
                String javaVersion = System.getProperty("java.version");
                if (javaVersion.startsWith("1.8")) {
                    return null;
                } else {
                    Method getPlatformClassLoader = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader", new Class[]{});
                    return (ClassLoader) getPlatformClassLoader.invoke(null);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

}
