package rocks.inspectit.ocelot.core.config.propertysources;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertiesPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.InetAddress;
import java.util.Properties;

/**
 * This property source populates all settings of {@link rocks.inspectit.ocelot.config.model.env.EnvironmentSettings}.
 */
@Slf4j
public class EnvironmentInformationPropertySource extends PropertiesPropertySource {

    public EnvironmentInformationPropertySource(String name) {
        super(name, getEnvironmentProperties());
    }

    private static Properties getEnvironmentProperties() {
        Properties result = new Properties();
        result.put("inspectit.env.agent-dir", getAgentJarDirectory());
        result.put("inspectit.env.host", getHostName());
        result.put("inspectit.env.pid", getPid());
        return result;
    }

    /**
     * @return The path where the ocelot agent jar is placed (without a leading slash)
     */
    private static String getAgentJarDirectory() {
        URL agentJar = Instances.AGENT_JAR_URL;
        if (agentJar != null) {
            try {
                return new File(agentJar.toURI()).getParent();
            } catch (Exception e) {
                log.error("Error detecting JAR directory {}", e);
            }
        }
        //fallback to the "temp" directory
        return getTempDir();
    }

    /**
     * For "inspectit.env.agent-dir" we fallback to the temp directory if the agent jar is not found.
     * In reality, this happens only during unit and integration tests.
     *
     * @return
     */
    private static String getTempDir() {
        String tempdir = System.getProperty("java.io.tmpdir");
        if (StringUtils.isBlank(tempdir)) {
            return "";
        }
        if (tempdir.endsWith("/") || tempdir.endsWith("\\")) {
            return tempdir.substring(0, tempdir.length() - 1);
        } else {
            return tempdir;
        }
    }

    /**
     * @return The hostname where the ocelot agent is running.
     */
    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.debug("Failed to resolve hostname {}", e);
            return "unknown";
        }
    }

    /**
     * @return The process id of the running JVM.
     */
    private static String getPid() {
        try{
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (Exception e) {
            log.debug("Failed to resolve process id {}", e);
            return "unknown";
        }
    }
}
