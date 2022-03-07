package rocks.inspectit.ocelot.core.config.propertysources.file;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link DirectoryPropertySource}
 */
public class DirectoryPropertySourceTest {

    private static final String tmpDir = "tmpconf_dirprop_test";

    private static final String testPropertySourceName = "test";

    private static Path testDirectory;

    private DirectoryPropertySource source;

    @Mock
    private MutablePropertySources psContainer;

    ArgumentCaptor<PropertySource> captor = ArgumentCaptor.forClass(PropertySource.class);



    @BeforeAll
    static void createConfigDir() throws IOException {
        testDirectory = Files.createTempDirectory(tmpDir);
    }

    @AfterAll
    static void deleteConfigDir() throws Exception {
        FileUtils.deleteDirectory(testDirectory.toFile());
    }

    @AfterEach
    void cleanConfigDir() throws Exception {
        FileUtils.cleanDirectory(testDirectory.toFile());
    }

    @BeforeEach
    void initDirectoryPropertySource() {
        // init DirectoryPropertySource
        source = new DirectoryPropertySource(testPropertySourceName, testDirectory);
        // init mock MutablePropertySources
        when(psContainer.stream()).thenReturn(Stream.empty());
    }

    /**
     * Test class for {@link DirectoryPropertySource#reload(MutablePropertySources)}
     */
    @DirtiesContext
    @Nested
    @ExtendWith(MockitoExtension.class)
    class Reload {

        @Test
        public void reloadConfigurations() throws IOException {
            Files.createFile(testDirectory.resolve("first.yaml"));
            Files.createFile(testDirectory.resolve("second.properties"));
            Files.createFile(testDirectory.resolve("third.json"));
            Files.write(testDirectory.resolve("third.json"), "{}".getBytes()); // empty json object

            source.reload(psContainer);

            verify(psContainer, times(3)).addAfter(anyString(), captor.capture());
            verifyNoMoreInteractions(psContainer);

            assertThat(captor.getAllValues()).extracting(PropertySource::getName)
                    .contains("test/first.yaml", "test/second.properties", "test/third.json");
        }

        @Test
        public void reloadWithExceptions() throws IOException {
            Files.createFile(testDirectory.resolve("a_third.json")); // invalid JSON/JSON file
            Files.write(testDirectory.resolve("a_third.json"), "{,,,}".getBytes()); // invalid YAML/JSON

            Files.createFile(testDirectory.resolve("b_second.properties"));
            Files.createFile(testDirectory.resolve("c_first.yaml"));

            when(psContainer.stream()).thenReturn(Stream.empty());

            source.reload(psContainer);

            verify(psContainer, times(2)).addAfter(anyString(), captor.capture());
            verifyNoMoreInteractions(psContainer);

            assertThat(captor.getAllValues()).extracting(PropertySource::getName)
                    .contains("test/c_first.yaml", "test/b_second.properties");
        }
    }

    /**
     * Test class for {@link DirectoryPropertySource#loadContentsToPropertySources()} using direct access to a created {@link DirectoryPropertySource}
     */
    @Nested
    @DirtiesContext
    @ExtendWith(MockitoExtension.class)
    class LoadContentsToPropertySources {

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
            Files.write(testDirectory.resolve(fileName), data.getBytes(Charset.defaultCharset()));
            // reload DirectoryPropertySource
            source.reload(psContainer);
            verify(psContainer, times(1)).addAfter(anyString(), captor.capture());
            verifyNoMoreInteractions(psContainer);

            PropertySource<InspectitConfig> ps = captor.getAllValues().get(0);
            assertThat(ps).isNotNull();
            // assert new service name
            Awaitility.await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(serviceName).isEqualTo(ps.getProperty("inspectit.service-name")));
        }
    }

}


