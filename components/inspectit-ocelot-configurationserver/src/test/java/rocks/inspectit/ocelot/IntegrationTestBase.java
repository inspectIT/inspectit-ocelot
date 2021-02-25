package rocks.inspectit.ocelot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;
import rocks.inspectit.ocelot.mappings.AgentMappingManager;
import rocks.inspectit.ocelot.mappings.AgentMappingSerializer;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create",})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = IntegrationTestBase.Initializer.class)
public class IntegrationTestBase {

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            try {
                Path tempDirectory = Files.createTempDirectory("ocelot");
                FileUtils.forceDeleteOnExit(tempDirectory.toFile());

                TestPropertyValues.of("inspectit-config-server.working-directory=" + tempDirectory.toAbsolutePath()
                        .toString()).applyTo(context.getEnvironment());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected InspectitServerSettings settings;

    @Autowired
    protected FileManager fileManager;

    /**
     * Authenticated restTemplate;
     */
    protected TestRestTemplate authRest;

    @BeforeEach
    void setup() throws GitAPIException, IOException {
        // enforce a the same state for each test
        FileUtils.cleanDirectory(new File(settings.getWorkingDirectory()));

        authRest = rest.withBasicAuth(settings.getDefaultUser().getName(), settings.getDefaultUser().getPassword());

        // we clean the working directory after each test, thus, we have to reinitialize the working directory, otherwise
        // it is not a git repository anymore

        // recreate the default agent mappings - the AgentMappingSerializer is not used because its depending on the version manager
        AgentMapping defaultMapping = (AgentMapping) ReflectionTestUtils.getField(AgentMappingManager.class, "DEFAULT_MAPPING");
        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        ymlMapper.findAndRegisterModules();
        Path agentMappingsFile = Paths.get(settings.getWorkingDirectory(), AbstractFileAccessor.AGENT_MAPPINGS_FILE_NAME);
        String mappingsAsYaml = ymlMapper.writeValueAsString(Collections.singletonList(defaultMapping));
        Files.write(agentMappingsFile, mappingsAsYaml.getBytes());

        // init the version manager
        VersioningManager versioningManager = (VersioningManager) ReflectionTestUtils.getField(fileManager, "versioningManager");
        versioningManager.initialize();
    }

    @AfterEach
    void afterEach() throws IOException {
        FileUtils.cleanDirectory(new File(settings.getWorkingDirectory()));
    }
}
