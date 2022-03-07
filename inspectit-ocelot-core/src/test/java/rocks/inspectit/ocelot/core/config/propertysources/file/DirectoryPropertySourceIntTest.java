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
 * Integration test class for {@link DirectoryPropertySource}
 */
public class DirectoryPropertySourceIntTest {

    private static final String tmpDir = "tmpconf_dirprop_test";

    @BeforeAll
    static void createConfigDir() {
        new File(tmpDir).mkdir();
    }

    @AfterEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir));
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(new File(tmpDir));
    }

    /**
     * Integration test class for {@link DirectoryPropertySource#loadContentsToPropertySources()} using {@link SpringTestBase}
     */
    @Nested
    @DirtiesContext
    @TestPropertySource(properties = {"inspectit.config.file-based.path=" + tmpDir, "inspectit.config.file-based.frequency=50ms"})
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
