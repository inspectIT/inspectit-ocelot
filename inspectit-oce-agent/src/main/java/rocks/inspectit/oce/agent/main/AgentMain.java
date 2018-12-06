package rocks.inspectit.oce.agent.main;

import rocks.inspectit.oce.agent.AgentImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Entry point of the agent.
 *
 * @author Jonas Kunz
 */
public class AgentMain {

    private static final String INSPECTIT_JARS_LIST_FILE = "/inspectit-jars.txt";

    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            //Check if the IAgent interface is found at the bootstrap loader
            Class.forName("rocks.inspectit.oce.agent.bootstrap.IAgent");
            System.out.println("Agent already started - aborting initialization");
        } catch (ClassNotFoundException e) {
            try {
                initializeClassPathAndAgent(inst);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            initializeClassPathAndAgent(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * invokes {@link #buildClasspath(Instrumentation inst)} and then calls {@link AgentImpl#createInstance()}.
     *
     * @throws IOException if something goes wrong with the initialization
     */
    private static void initializeClassPathAndAgent(Instrumentation inst) throws IOException {
        InspectITClassLoader icl = buildClasspath(inst);

        //the AgentImpl resides in the inspectIT classloader, therefore it cannot be referenced directly
        Class<?> agentImplClass;
        try {
            agentImplClass = Class.forName("rocks.inspectit.oce.agent.AgentImpl", true, icl);
            agentImplClass.getMethod("createInstance").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to locate or invoke AgentImpl in InspectIT classloader", e);
        }
    }

    /**
     * Parses the inspectit-jars.txt file found in the agent jar.
     * Afterwards loads the jars from /bootstrap/ to the bootstraploader and from /libs/ to the inspectIT classloader
     *
     * @return the created inspectIT classloader
     * @throws IOException
     */
    private static InspectITClassLoader buildClasspath(Instrumentation inst) throws IOException {
        List<Path> bootstrapJars = new ArrayList<>();
        List<Path> privateJars = new ArrayList<>();

        extractNestedJars(bootstrapJars, privateJars);

        for (Path jarPath : bootstrapJars) {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(jarPath.toFile()));
        }

        List<URL> privateJarURLs = new ArrayList<>();
        for (Path jarPath : privateJars) {
            URL jarUrl = jarPath.toUri().toURL();
            privateJarURLs.add(jarUrl);
        }
        URL[] privateJarURLsArray = privateJarURLs.toArray(new URL[]{});
        return new InspectITClassLoader(privateJarURLsArray);
    }

    /**
     * Extracts the jars listed in the resource file {@link #INSPECTIT_JARS_LIST_FILE} to temporary jar files.     *
     *
     * @param bootstrapJars extracted jars from /bootstrap/ are appended to this collection
     * @param privateJars   extracted jars from /libs/ are appended to this collection
     * @throws IOException
     */
    private static void extractNestedJars(Collection<Path> bootstrapJars, Collection<Path> privateJars) throws IOException {
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(
                             AgentMain.class.getResourceAsStream(INSPECTIT_JARS_LIST_FILE)))) {
            String resourcePath;
            while ((resourcePath = br.readLine()) != null) {
                if (resourcePath.startsWith("libs/")) {
                    privateJars.add(copyResourceToTempJarFile("/" + resourcePath));
                } else if (resourcePath.startsWith("bootstrap/")) {
                    bootstrapJars.add(copyResourceToTempJarFile("/" + resourcePath));
                }
            }
        }
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
    private static class InspectITClassLoader extends URLClassLoader {

        InspectITClassLoader(URL[] urls) {
            super(urls, findParentClassLoader());
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