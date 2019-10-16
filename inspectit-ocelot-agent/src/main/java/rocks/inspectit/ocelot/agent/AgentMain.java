package rocks.inspectit.ocelot.agent;

import rocks.inspectit.ocelot.bootstrap.AgentManager;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
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
    private static final String INSPECTIT_CORE_JAR_PATH = "/inspectit-ocelot-core.jar";
    private static final String OPENCENSUS_FAT_JAR_PATH = "/opencensus-fat.jar";
    private static final String PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY = "inspectit.publishOpenCensusToBootstrap";

    /**
     * Main method for attaching the agent itself to a running JVM.
     *
     * @param args the pid of a JVM
     */
    public static void main(String[] args) {
        boolean error = false;
        if (args.length != 1 && args.length != 2) {
            error = true;
        } else if (!args[0].matches("\\d+")) {
            error = true;
        }

        if (error) {
            System.err.println("Please specify the PID of the JVM you want the agent attach to.\nNote: you can pass properties to the agent represented as a JSON string!\n\nUsage: <PID> [AGENT_PROPERTIES]");
            System.exit(1);
        }

        String agentProperties = args.length == 2 ? args[1] : null;
        AgentAttacher.attach(Integer.parseInt(args[0]), agentProperties);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        //TODO: currently replacing the agent does not really work as all Agent versions share the same namespace in the same classpath
        startAgent(agentArgs, inst, true);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        boolean loadOpenCensusToBootstrap = "true".equalsIgnoreCase(System.getProperty(PUBLISH_OPEN_CENSUS_TO_BOOTSTRAP_PROPERTY));
        try {
            if (loadOpenCensusToBootstrap) {
                Path ocJarFile = copyResourceToTempJarFile(OPENCENSUS_FAT_JAR_PATH);
                inst.appendToBootstrapClassLoaderSearch(new JarFile(ocJarFile.toFile()));
            }
            //we make sure that the startup of inspectIT is asynchronous
            new Thread(() ->
                    startAgent(agentArgs, inst, !loadOpenCensusToBootstrap)
            ).start();
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

    /**
     * Loads {@link #INSPECTIT_BOOTSTRAP_JAR_PATH} with the bootstrap classloader and @link {@link #INSPECTIT_CORE_JAR_PATH} with a new inspectIT loader.
     *
     * @return the created inspectIT classloader
     * @throws IOException
     */
    private static InspectITClassLoader initializeInspectitLoader(Instrumentation inst, boolean includeOpenCensus) throws IOException {
        Path bootstrapJar = copyResourceToTempJarFile(INSPECTIT_BOOTSTRAP_JAR_PATH);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar.toFile()));

        Instances.BOOTSTRAP_JAR_URL = bootstrapJar.toUri().toURL();

        Instances.AGENT_JAR_URL = AgentMain.class.getProtectionDomain().getCodeSource().getLocation();

        Path coreJar = copyResourceToTempJarFile(INSPECTIT_CORE_JAR_PATH);
        InspectITClassLoader icl = new InspectITClassLoader(new URL[]{coreJar.toUri().toURL()});

        if (includeOpenCensus) {
            Path ocJarFile = copyResourceToTempJarFile(OPENCENSUS_FAT_JAR_PATH);
            icl.addURL(ocJarFile.toUri().toURL());
        }

        return icl;
    }

    /**
     * Copies the given resource to a new temporary file with the ending ".jar"
     *
     * @param resourcePath the path to the resource
     * @return the path to the generated jar file
     * @throws IOException
     */
    private static Path copyResourceToTempJarFile(String resourcePath) throws IOException {
        try (InputStream is = AgentMain.class.getResourceAsStream(resourcePath)) {
            Path targetFile = Files.createTempFile("", ".jar");
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            targetFile.toFile().deleteOnExit();
            return targetFile;
        }
    }

    /**
     * private inspectIT classloader to ensure our classes are hidden from other application classes
     */
    public static class InspectITClassLoader extends URLClassLoader {

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
                    return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader", new Class[]{}).invoke(null);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

}