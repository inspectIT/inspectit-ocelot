package rocks.inspectit.ocelot.agent;

import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.jar.JarFile;

/**
 * Entry point of the agent.
 *
 * @author Jonas Kunz
 */
public class AgentMain {

    private static final String INSPECTIT_BOOTSTRAP_JAR_PATH = "/inspectit-ocelot-bootstrap.jar";

    private static final String INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX = "ocelot-bootstrap-";

    private static final String INSPECTIT_CORE_JAR_PATH = "/inspectit-ocelot-core.jar";

    private static final String INSPECTIT_CORE_JAR_TEMP_PREFIX = "ocelot-core-";

    /**
     * Deprecated as of version 2.X. Use {@link #PUBLISH_OPEN_TELEMETRY_TO_BOOTSTRAP_PROPERTY} instead.
     */
    @Deprecated
    private static final String PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY = "inspectit.publishOpenCensusToBootstrap";

    private static final String OPEN_TELEMETRY_FAT_JAR_PATH = "/opentelemetry-fat.jar";

    private static final String OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX = "ocelot-opentelemetry-fat-";

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

    // invoked if agent is started after the application
    public static void agentmain(String agentArgs, Instrumentation inst) {
        //TODO: currently replacing the agent does not really work as all Agent versions share the same namespace in the same classpath
        if (!isAsyncInstrumentationEnabled()) {
            System.out.println(ASYNC_INSTRUMENTATION_PROPERTY + " and " + ASYNC_INSTRUMENTATION_ENV_PROPERTY + " are ignored when when attaching agent to a running application!");
        }
        startAgent(agentArgs, inst, true);
    }

    // invoked if agent is started before the application
    public static void premain(String agentArgs, Instrumentation inst) {
        boolean loadOpenTelemetryJarToBootstrap = null != System.getProperty(PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY) ? "true".equalsIgnoreCase(System.getProperty(PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY)) : "true".equalsIgnoreCase(System.getProperty(PUBLISH_OPEN_TELEMETRY_TO_BOOTSTRAP_PROPERTY));
        // check for deprecated JVM property
        if (null != System.getProperty(PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY)) {
            System.err.println("You are using the deprecated JVM property '" + PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY + "'. Please use the new JVM property '" + PUBLISH_OPEN_TELEMETRY_TO_BOOTSTRAP_PROPERTY + "'. inspectIT Ocelot has moved from OpenCensus to OpenTelemetry. However, applications using the OpenCensusAPI are still supported through the opentelemetry-opencensus-shim ");
        }

        try {
            if (loadOpenTelemetryJarToBootstrap) {
                Path otelJarFile = copyResourceToTempJarFile(OPEN_TELEMETRY_FAT_JAR_PATH, OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX);
                inst.appendToBootstrapClassLoaderSearch(new JarFile(otelJarFile.toFile()));
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

    private static void startAgent(String agentArgs, Instrumentation inst, boolean includeOpencensusInInspectitLoader) {
        try {
            InspectITClassLoader icl = initializeInspectitLoader(inst, includeOpencensusInInspectitLoader);
            AgentManager.startOrReplaceInspectitCore(icl, agentArgs, inst);
        } catch (Exception e) {
            System.err.println("Error starting inspectIT Agent!");
            e.printStackTrace();
        }
    }

    private static boolean isAsyncInstrumentationEnabled() {
        String isAsyncInstrumentationPropertyValue = null != System.getProperty(ASYNC_INSTRUMENTATION_PROPERTY) ? System.getProperty(ASYNC_INSTRUMENTATION_PROPERTY) : System.getenv(ASYNC_INSTRUMENTATION_ENV_PROPERTY);
        return null == isAsyncInstrumentationPropertyValue || "true".equalsIgnoreCase(isAsyncInstrumentationPropertyValue);
    }

    /**
     * Loads {@link #INSPECTIT_BOOTSTRAP_JAR_PATH} with the bootstrap classloader and @link {@link #INSPECTIT_CORE_JAR_PATH} with a new inspectIT loader.
     *
     * @return the created inspectIT classloader
     *
     * @throws IOException
     */
    private static InspectITClassLoader initializeInspectitLoader(Instrumentation inst, boolean includeOpenTelemetry) throws IOException {
        Path bootstrapJar = copyResourceToTempJarFile(INSPECTIT_BOOTSTRAP_JAR_PATH, INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar.toFile()));

        Instances.BOOTSTRAP_JAR_URL = bootstrapJar.toUri().toURL();

        Instances.AGENT_JAR_URL = AgentMain.class.getProtectionDomain().getCodeSource().getLocation();

        Path coreJar = copyResourceToTempJarFile(INSPECTIT_CORE_JAR_PATH, INSPECTIT_CORE_JAR_TEMP_PREFIX);
        InspectITClassLoader icl = new InspectITClassLoader(new URL[]{coreJar.toUri().toURL()});

        if (includeOpenTelemetry) {
            Path otJarFile = copyResourceToTempJarFile(OPEN_TELEMETRY_FAT_JAR_PATH, OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX);
            icl.addURL(otJarFile.toUri().toURL());
        }

        return icl;
    }

    /**
     * Copies the given resource to a new temporary file with the ending ".jar"
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the new temporary file
     *
     * @return the path to the generated jar file
     *
     * @throws IOException
     */
    private static Path copyResourceToTempJarFile(String resourcePath, String prefix) throws IOException {
        try (InputStream is = AgentMain.class.getResourceAsStream(resourcePath)) {
            Path targetFile = Files.createTempFile(prefix, ".jar");
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            targetFile.toFile().deleteOnExit();
            return targetFile;
        }
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
