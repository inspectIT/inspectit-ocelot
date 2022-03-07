package rocks.inspectit.ocelot.core.config.propertysources.file;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link DirectoryPropertySource}
 */
public class DirectoryPropertySourceTest {

    private static final String tmpDir = "tmpconf_dirprop_test";

    @BeforeAll
    static void createConfigDir() {
        new File(tmpDir).mkdir();
    }

    @BeforeEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir));
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(new File(tmpDir));
    }

    /**
     * Test class for {@link DirectoryPropertySource#loadContentsToPropertySources()}
     */
    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.config.file-based.path=" + tmpDir, "inspectit.config.file-based.frequency=50ms"})
    class LoadContentsToPropertySources extends SpringTestBase {

        @Autowired
        InspectitEnvironment inspectitEnvironment;

        private static final String defaultServiceName = "InspectIT Agent";

        @BeforeEach
        void testDefaultServiceName() {
            updateProperties(mps -> {
                mps.setProperty("inspectit.config.service-name", defaultServiceName);
            });
            Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() ->
                    // assert that the service name is still 'InspectIT Agent'
                    assertThat(defaultServiceName).isEqualTo(inspectitEnvironment.getCurrentConfig().getServiceName()));
        }

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
            String pathName = tmpDir + "/" + fileName;
            FileUtils.write(new File(pathName), data, Charset.defaultCharset());
            // assert new service name
            Awaitility.await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(serviceName).isEqualTo(inspectitEnvironment.getCurrentConfig()
                            .getServiceName()));
        }
    }

}
