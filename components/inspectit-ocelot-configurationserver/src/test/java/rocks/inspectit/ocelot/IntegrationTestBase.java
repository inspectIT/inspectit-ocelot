package rocks.inspectit.ocelot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        // "inspectit-config-server.working-directory=" + tempDirectory,
        "spring.datasource.url=jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = IntegrationTestBase.Initializer.class)
public class IntegrationTestBase {

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            try {
                Path tempDirectory = Files.createTempDirectory("ocelot");
                FileUtils.forceDeleteOnExit(tempDirectory.toFile());

                TestPropertyValues.of("inspectit-config-server.working-directory=" + tempDirectory.toAbsolutePath().toString())
                        .applyTo(context.getEnvironment());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected InspectitServerSettings settings;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Authenticated restTemplate;
     */
    protected TestRestTemplate authRest;

    @BeforeEach
    void setupAuthentication() {
        authRest = rest.withBasicAuth(settings.getDefaultUser().getName(), settings.getDefaultUser().getPassword());
    }

    @AfterEach
    void afterEach() throws IOException {
        FileUtils.cleanDirectory(new File(settings.getWorkingDirectory()));
    }

    protected void createTestFiles(String... files) {
        try {
            Path tempDirectory = Paths.get(settings.getWorkingDirectory());
            for (String file : files) {
                String path;
                String content;
                if (file.contains("=")) {
                    String[] splitted = file.split("=");
                    path = splitted[0];
                    content = splitted.length == 2 ? splitted[1] : "";
                } else {
                    path = file;
                    content = "";
                }

                Path targetFile = tempDirectory.resolve(path);
                Files.createDirectories(targetFile.getParent());
                Files.write(targetFile, content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
