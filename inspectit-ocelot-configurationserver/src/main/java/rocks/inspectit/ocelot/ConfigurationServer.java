package rocks.inspectit.ocelot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application class of the configuration server.
 */
@SpringBootApplication
public class ConfigurationServer {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationServer.class, args);
    }

}
