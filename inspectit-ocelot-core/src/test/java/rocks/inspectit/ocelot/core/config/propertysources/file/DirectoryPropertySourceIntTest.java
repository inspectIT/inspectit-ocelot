package rocks.inspectit.ocelot.core.config.propertysources.file;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class for {@link DirectoryPropertySource}
 */
public class DirectoryPropertySourceIntTest {

    private static Path TEMP_DIRECTORY;

    @BeforeAll
    static void createConfigDir() throws IOException {
        TEMP_DIRECTORY = Files.createTempDirectory("Ocelot_DirectoryPropertySourceIntTest");
    }

    @AfterEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(TEMP_DIRECTORY.toFile());
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(TEMP_DIRECTORY.toFile());
    }

    /**
     * Injection of test properties.
     */
    static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            //@formatter:off
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "inspectit.config.file-based.path=" + TEMP_DIRECTORY.toAbsolutePath());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "inspectit.config.file-based.frequency=50ms");
            //@formatter:on
        }
    }

    /**
     * Integration test class for {@link DirectoryPropertySource#loadContentsToPropertySources()} using {@link SpringTestBase}
     */
    @Nested
    @DirtiesContext
    @ContextConfiguration(initializers = DirectoryPropertySourceIntTest.ContextInitializer.class)
    class LoadContentsToPropertySources extends SpringTestBase {

        @Autowired
        InspectitEnvironment inspectitEnvironment;

        /**
         * Tests that JSON configurations are successfully read
         *
         * @throws IOException
         */
        @DirtiesContext
        @Test
        void testLoadJson() throws IOException {
            String serviceName = "serviceNameFromJSON";
            writeConfigurationAndAssertServiceName("DirectoryPropertySourceTest.json", "{\"inspectit\":{\"service-name\":" + serviceName + "}}", serviceName);
        }

        /**
         * Tests that YAML configurations are successfully read
         *
         * @throws IOException
         */
        @DirtiesContext
        @Test
        void testLoadYaml() throws IOException {
            String serviceName = "serviceNameFromYAML";
            writeConfigurationAndAssertServiceName("DirectoryPropertySourceTest.yml", "inspectit:\n   service-name: " + serviceName, serviceName);
        }

        /**
         * Writes the given configuration to a temporary file and tests that the {@link rocks.inspectit.ocelot.config.model.InspectitConfig#serviceName} equals the given service name.
         *
         * @param fileName    the name of the temporary file to write to
         * @param data        the data to write
         * @param serviceName the expected service name
         *
         * @throws IOException
         */
        private void writeConfigurationAndAssertServiceName(String fileName, String data, String serviceName) throws IOException {
            // write data to the file
            Path targetFile = TEMP_DIRECTORY.resolve(fileName);
            FileUtils.write(targetFile.toFile(), data, Charset.defaultCharset());
            // assert new service name
            Awaitility.await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(serviceName).isEqualTo(inspectitEnvironment.getCurrentConfig()
                            .getServiceName()));
        }
    }

}
