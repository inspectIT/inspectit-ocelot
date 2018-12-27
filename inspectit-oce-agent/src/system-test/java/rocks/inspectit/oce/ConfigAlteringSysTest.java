package rocks.inspectit.oce;

import org.junit.jupiter.api.AfterEach;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Test base class which allows the modification of agent config properties.
 */
public class ConfigAlteringSysTest {

    private static final Path configFile = Paths.get("inspectit-systest.properties");

    /**
     * Overwrites default or previously set properties.
     *
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        Properties props = new Properties();
        if (configFile.toFile().exists()) {
            try (FileInputStream fin = new FileInputStream(configFile.toFile())) {
                props.load(fin);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        properties.forEach(props::setProperty);
        try (FileOutputStream fout = new FileOutputStream(configFile.toFile(), false)) {
            props.store(fout, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Thread.sleep(700); //ensure that the config is restored
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Overwrites default or previously set properties.
     *
     * @param keyValuePairs the properties to set, keys followed by their values
     */
    public void setProperties(String... keyValuePairs) {
        HashMap<String, String> props = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            props.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        setProperties(props);
    }

    /**
     * Resets all properties to default.
     *
     * @throws Exception
     */
    @AfterEach
    public void clearProperties() {
        if (configFile.toFile().exists()) {
            configFile.toFile().delete();
            try {
                Thread.sleep(700); //ensure that the config is restored
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
