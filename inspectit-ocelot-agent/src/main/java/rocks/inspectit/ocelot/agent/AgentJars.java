package rocks.inspectit.ocelot.agent;

import rocks.inspectit.ocelot.bootstrap.AgentProperties;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Class for creating and reading additional jar files.
 * The agent-jar itself contains multiple jars, which contain classes that have to be loaded via the
 * bootstrap classloader or the inspectIT classloader. Since files within the agent-jar are not considered
 * part of the file system, these jar files have to be copied outside the agent-jar to be accessible.
 */
public class AgentJars {

    private static final String INSPECTIT_BOOTSTRAP_JAR_PATH = "/inspectit-ocelot-bootstrap.jar";

    private static final String INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX = "ocelot-bootstrap-";

    private static final String INSPECTIT_CORE_JAR_PATH = "/inspectit-ocelot-core.jar";

    private static final String INSPECTIT_CORE_JAR_TEMP_PREFIX = "ocelot-core-";

    private static final String OPEN_TELEMETRY_FAT_JAR_PATH = "/opentelemetry-fat.jar";

    private static final String OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX = "ocelot-opentelemetry-fat-";

    /** The file used to load the agent version */
    private static final String AGENT_VERSION_INFORMATION_FILE = "/ocelot-version.info";

    private static String agentVersion;

    /**
     * Get the path to the inspectit-ocelot-bootstrap.jar, which contains all classes from inspectit-ocelot-bootstrap.
     *
     * @return the absolute path used for the inspectit-ocelot-bootstrap.jar in the file system
     */
    public static Path getOcelotBootstrapJar() throws IOException {
        return getJar(INSPECTIT_BOOTSTRAP_JAR_PATH, INSPECTIT_BOOTSTRAP_JAR_TEMP_PREFIX);
    }

    /**
     * Get the path to the inspectit-ocelot-core.jar, which contains all classes from inspectit-ocelot-core,
     * inspectit-ocelot-config and inspectit-ocelot-sdk.
     *
     * @return the absolute path used for the inspectit-ocelot-core.jar in the file system
     */
    public static Path getOcelotCoreJar() throws IOException {
        return getJar(INSPECTIT_CORE_JAR_PATH, INSPECTIT_CORE_JAR_TEMP_PREFIX);
    }

    /**
     * Get the path to the opentelemetry-fat.jar, which contains classes for OpenTelemetry as well as OpenCensus.
     *
     * @return the absolute path used for the opentelemetry-fat.jar in the file system
     */
    public static Path getOpenTelemetryJar() throws IOException {
        return getJar(OPEN_TELEMETRY_FAT_JAR_PATH, OPEN_TELEMETRY_FAT_JAR_TEMP_PREFIX);
    }

    /**
     * Copies the resource to the file system or recycles an existing file.
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the file
     * @return the absolute path used for the specified jar file in the file system
     */
    private static Path getJar(String resourcePath, String prefix) throws IOException {
        if(isRecyclingEnabled())
            return recycleJarFile(resourcePath, prefix);
        return copyResourceToTempJarFile(resourcePath, prefix);
    }

    /**
     * Tries to recycle a jar file. If no file has been found, a new one will be created.
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the file
     * @return the path to the used jar file
     */
    private static Path recycleJarFile(String resourcePath, String prefix) throws IOException {
        Path jarPath = getAbsoluteJarPath(prefix);
        return recycleJarFileWithLock(resourcePath, jarPath);
    }

    /**
     * Copies the given resource to a new temporary jar file. The file should be deleted after shutdown.
     * This is necessary, because files within our agent jar are not actually located in the file system.
     *
     * @param resourcePath the path to the resource
     * @param prefix       the name of the new temporary file
     *
     * @return the path to the generated temporary jar file
     */
    private static Path copyResourceToTempJarFile(String resourcePath, String prefix) throws IOException {
        try (InputStream is = AgentJars.class.getResourceAsStream(resourcePath)) {
            Path tempDir = getTempDirectory();
            Files.createDirectories(tempDir);
            Path targetFile = Files.createTempFile(tempDir, prefix, ".jar");

            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            targetFile.toFile().deleteOnExit();
            return targetFile;
        }
    }

    /**
     * Copies the given resource to a new jar file. The file will NOT be deleted after shutdown.
     * This is necessary, because files within our agent jar are not actually located in the file system.
     *
     * @param resourcePath the path to the resource
     * @param jarPath      the path of the new file
     *
     * @return the path to the generated jar file
     */
    private static Path copyResourceToJarFile(String resourcePath, Path jarPath) throws IOException  {
        try (InputStream is = AgentJars.class.getResourceAsStream(resourcePath)) {
            Files.copy(is, jarPath, StandardCopyOption.REPLACE_EXISTING);
            return jarPath;
        }
    }

    /**
     * Tries to recycle a jar file while using a file lock to prevent multiple processes from writing the file concurrently.
     * This could happen when multiple agents start at the same time and no jar file exists at the start.
     * Thus, it is only possible to access the jare file when acquiring the lock for the lock file.
     *
     * @param resourcePath the path to the resource, used if no jar has been found
     * @param jarPath the path of the jar file
     * @return the path to the used jar file
     */
    private static Path recycleJarFileWithLock(String resourcePath, Path jarPath) throws IOException {
        Files.createDirectories(jarPath.getParent());
        Path lockPath = jarPath.resolveSibling(jarPath.getFileName() + ".lock");

        try (RandomAccessFile lockFile = new RandomAccessFile(lockPath.toFile(), "rw");
             FileChannel lockChannel = lockFile.getChannel();
             FileLock lock = acquireLock(lockChannel, 1500, 50)
        ) {
            if (lock == null) throw new IOException("Could not acquire lock for: " + lockPath);

            if (Files.exists(jarPath))
                return jarPath;
            return copyResourceToJarFile(resourcePath, jarPath);
        }
    }

    /**
     * Tries to acquire the lock for accessing the jar files with retries until timeout.
     *
     * @param lockChannel the channel of the lock file
     * @param timeout the maximum time to wait in ms
     * @param retryDelay the delay for every retry in ms
     * @return the acquired lock or {@code null} if timed out
     */
    private static FileLock acquireLock(FileChannel lockChannel, long timeout, long retryDelay) throws IOException {
        long end = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < end) {
            try {
                FileLock lock = lockChannel.tryLock();
                if (lock != null) return lock;
            } catch (OverlappingFileLockException e) {
                // already locked in this JVM – shouldn't happen
                throw new IOException(e);
            }

            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for file lock", e);
            }
        }
        return null; // timed out
    }

    /**
     * @return the directory to use for temporary files of inspectIT
     */
    private static Path getTempDirectory() {
        String temporaryDirectoryValue = null != System.getProperty(AgentProperties.INSPECTIT_TEMP_DIR_PROPERTY) ?
                System.getProperty(AgentProperties.INSPECTIT_TEMP_DIR_PROPERTY) : System.getenv(AgentProperties.INSPECTIT_TEMP_DIR_ENV_PROPERTY);

        if(temporaryDirectoryValue != null)
            return Paths.get(temporaryDirectoryValue, "/inspectit-ocelot");

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
     * If the recycling of jars is enabled, we will search for existing jar files.
     * If no files could be found, we will create new ones. Otherwise, the existing files will be used.
     *
     * @return true, if jars should be recycled
     */
    private static boolean isRecyclingEnabled() {
        String isRecyclingEnabledValue = null != System.getProperty(AgentProperties.RECYCLE_JARS_PROPERTY) ?
                System.getProperty(AgentProperties.RECYCLE_JARS_PROPERTY) : System.getenv(AgentProperties.RECYCLE_JARS_ENV_PROPERTY);
        return "true".equalsIgnoreCase(isRecyclingEnabledValue);
    }

    /**
     * Reads the current agent version from the ocelot-version.info file
     *
     * @return the current agent version
     */
    private static String getAgentVersion() {
        if(agentVersion == null) {
            try (InputStream is = AgentJars.class.getResourceAsStream(AGENT_VERSION_INFORMATION_FILE)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                agentVersion = reader.readLine();
            } catch (Exception e) {
                System.err.println("Could not read agent version information file");
                agentVersion = "UNKNOWN";
            }
        }
        return agentVersion;
    }
}
