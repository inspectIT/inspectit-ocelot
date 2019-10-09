package rocks.inspectit.ocelot.core.config.propertysources;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertiesPropertySource;
import rocks.inspectit.ocelot.bootstrap.Instances;

import java.io.File;
import java.net.URL;
import java.util.Properties;

@Slf4j
public class EnvironmentInformationPropertySource extends PropertiesPropertySource {

    public EnvironmentInformationPropertySource(String name) {
        super(name, getEnvironmentProperties());
    }

    private static Properties getEnvironmentProperties() {
        Properties result = new Properties();
        result.put("inspectit.env.jar-dir", getAgentJarDirectory());
        return result;
    }

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
}
