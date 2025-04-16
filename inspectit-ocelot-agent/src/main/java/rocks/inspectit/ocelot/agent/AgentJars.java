package rocks.inspectit.ocelot.agent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Class for creating and retrieving additional jar files.
 */
public class AgentJars {

    // system and environment properties
    private static final String RECYCLE_JARS_PROPERTY = "inspectit.recycle-jars";

    private static final String RECYCLE_JARS_ENV_PROPERTY = "INSPECTIT_RECYCLE_JARS";

    private static final String INSPECTIT_TEMP_DIR_PROPERTY = "inspectit.temp-dir";

    private static final String INSPECTIT_TEMP_DIR_ENV_PROPERTY = "INSPECTIT_TEMP_DIR";

    // file names
    private static final String INSPECTIT_BOOTSTRAP_JAR_PATH = "/inspectit-ocelot-bootstrap.jar";

    private static final String INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX = "ocelot-bootstrap-";

    private static final String INSPECTIT_CORE_JAR_PATH = "/inspectit-ocelot-core.jar";

    private static final String INSPECTIT_CORE_JAR_TEMP_PREFIX = "ocelot-core-";

    private static final String OPEN_TELEMETRY_FAT_JAR_PATH = "/opentelemetry-fat.jar";

    private static final String OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX = "ocelot-opentelemetry-fat-";

    /**
     * @return the absolute path used for the inspectit-ocelot-bootstrap.jar
     */
    public static Path getOcelotBootstrapJar() throws IOException {
        return getJar(INSPECTIT_BOOTSTRAP_JAR_PATH, INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX);
    }

    /**
     * @return the absolute path used for the inspectit-ocelot-core.jar
     */
    public static Path getOcelotCoreJar() throws IOException {
        return getJar(INSPECTIT_CORE_JAR_PATH, INSPECTIT_CORE_JAR_TEMP_PREFIX);
    }

    /**
     * @return the absolute path used for the opentelemetry-fat.jar
     */
    public static Path getOpenTelemetryJar() throws IOException {
        return getJar(OPEN_TELEMETRY_FAT_JAR_PATH, OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX);
    }

    private static Path getJar(String resourcePath, String prefix) throws IOException {
        if(isRecyclingEnabled())
            return recycleJarFile(resourcePath, prefix);
        return copyResourceToTempJarFile(resourcePath, prefix);
    }

    /**
     * Tries to recycle JAR file. If no file have been found, a new one will be created.
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the file
     * @return the path to the used jar file
     */
    private static Path recycleJarFile(String resourcePath, String prefix) throws IOException {
        Path jarPath = getAbsoluteJarPath(prefix);
        if (Files.exists(jarPath))
            return jarPath;
        return copyResourceToJarFile(resourcePath, jarPath);
    }

    /**
     * Copies the given resource to a new jar file. The file will not be deleted after shutdown.
     *
     * @param resourcePath the path to the resource
     * @param jarPath      the path of the new file
     *
     * @return the path to the generated jar file
     */
    private static Path copyResourceToJarFile(String resourcePath, Path jarPath) throws IOException  {
        try (InputStream is = AgentMain.class.getResourceAsStream(resourcePath)) {
            Files.createDirectories(jarPath.getParent());
            Files.copy(is, jarPath, StandardCopyOption.REPLACE_EXISTING);
            return jarPath;
        }
    }

    /**
     * Copies the given resource to a new temporary jar file. The file should be deleted after shutdown.
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the new temporary file
     *
     * @return the path to the generated temporary jar file
     */
    private static Path copyResourceToTempJarFile(String resourcePath, String prefix) throws IOException {
        try (InputStream is = AgentMain.class.getResourceAsStream(resourcePath)) {
            Path tempDir = getTempDirectory();
            Path targetFile = Files.createTempFile(tempDir, prefix, ".jar");

            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            targetFile.toFile().deleteOnExit();
            return targetFile;
        }
    }

    /**
     * @return the directory to use for temporary files of inspectIT
     */
    private static Path getTempDirectory() {
        String temporaryDirectoryValue = null != System.getProperty(INSPECTIT_TEMP_DIR_PROPERTY) ? System.getProperty(INSPECTIT_TEMP_DIR_PROPERTY) : System.getenv(INSPECTIT_TEMP_DIR_ENV_PROPERTY);

        if(temporaryDirectoryValue != null)
            return Paths.get(temporaryDirectoryValue);

        String defaultTempDir = System.getProperty("java.io.tmpdir") + "/inspectit-ocelot";
        return Paths.get(defaultTempDir);
    }

    /**
     * @param prefix the name of the new jar file
     * @return the absolute path to the jar file
     */
    private static Path getAbsoluteJarPath(String prefix) {
        String tempDir = getTempDirectory().toString();
        String agentVersion = getAgentVersion();
        String absolutePath = tempDir + "/" + agentVersion + "/" + prefix + agentVersion + ".jar";
        return Paths.get(absolutePath);
    }

    /**
     * If the recycling of jars if enabled, we will search for existing jar files.
     * If no files could be found, we will create new ones. Otherwise, the existing files will be used.
     *
     * @return true, if jars should be recycled
     */
    private static boolean isRecyclingEnabled() {
        String isRecyclingEnabledValue = null != System.getProperty(RECYCLE_JARS_PROPERTY) ? System.getProperty(RECYCLE_JARS_PROPERTY) : System.getenv(RECYCLE_JARS_ENV_PROPERTY);
        return "true".equalsIgnoreCase(isRecyclingEnabledValue);
    }

    /**
     * @return the current agent version
     */
    private static String getAgentVersion() {
        // TODO retrieve current agent version (like in ocelot-core?)
        return "2.6.9";
    }
}
