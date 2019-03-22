package rocks.inspectit.ocelot.agent;

import rocks.inspectit.ocelot.bootstrap.AgentManager;

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

    public static void agentmain(String agentArgs, Instrumentation inst) {
        //TODO: currently replacing the agent does not really work as all Agent versions share the same namespace in the same classpath
        premain(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            ClassLoader icl = initializeClasspath(inst);
            AgentManager.startOrReplaceInspectitCore(icl, agentArgs, inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads {@link #INSPECTIT_BOOTSTRAP_JAR_PATH} with the bootstrap classloader and @link {@link #INSPECTIT_CORE_JAR_PATH} with a new inspectIT loader.
     *
     * @return the created inspectIT classloader
     * @throws IOException
     */
    private static InspectITClassLoader initializeClasspath(Instrumentation inst) throws IOException {
        Path bootstrapJar = copyResourceToTempJarFile(INSPECTIT_BOOTSTRAP_JAR_PATH);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar.toFile()));

        Path coreJar = copyResourceToTempJarFile(INSPECTIT_CORE_JAR_PATH);
        InspectITClassLoader icl = new InspectITClassLoader(new URL[]{coreJar.toUri().toURL()});

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