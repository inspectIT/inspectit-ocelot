package rocks.inspectit.ocelot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;

/**
 * The application class of the configuration server.
 */
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
@Slf4j
public class ConfigurationServer {

    /**
     * Initializer which ensures that the working directory exists prior to initializing the spring context.
     * This is required because otherwise the SQLite DB can't create its database file.
     */
    private static final ApplicationContextInitializer<ConfigurableApplicationContext> WORKING_DIRECTORY_INITIALIZER = (ctx) -> {
        InspectitServerSettings settings = Binder.get(ctx.getEnvironment())
                .bind("inspectit-config-server", InspectitServerSettings.class)
                .get();
        try {
            FileUtils.forceMkdir(new File(settings.getWorkingDirectory()));
        } catch (Exception e) {
            log.error("Could not create working directory", e);
        }
    };

    public static void main(String[] args) {
        new SpringApplicationBuilder(ConfigurationServer.class).initializers(WORKING_DIRECTORY_INITIALIZER).run(args);
    }

}
