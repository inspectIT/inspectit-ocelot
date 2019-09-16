package rocks.inspectit.ocelot.core.config.propertysources;

import lombok.extern.slf4j.Slf4j;
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
        return System.getProperty("java.io.tmpdir");
    }
}
