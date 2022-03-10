package rocks.inspectit.ocelot.core.config.propertysources.file;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectoryPropertySourceTest {

    private DirectoryPropertySource source;

    private static Path testDirectory;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testDirectory = Files.createTempDirectory("ocelot");
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        source = new DirectoryPropertySource("test", testDirectory);
    }

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.cleanDirectory(testDirectory.toFile());
    }

    @AfterAll
    public static void afterAll() throws IOException {
        FileUtils.deleteDirectory(testDirectory.toFile());
    }

    @Nested
    class Reload {

        @Mock
        private MutablePropertySources psContainer;

        @Test
        public void reloadConfigurations() throws IOException {
            Files.createFile(testDirectory.resolve("first.yaml"));
            Files.createFile(testDirectory.resolve("second.properties"));
            Files.createFile(testDirectory.resolve("third.json"));
            Files.write(testDirectory.resolve("third.json"), "{}".getBytes()); // empty json object

            when(psContainer.stream()).thenReturn(Stream.empty());

            source.reload(psContainer);

            ArgumentCaptor<PropertySource> captor = ArgumentCaptor.forClass(PropertySource.class);
            verify(psContainer, times(3)).addAfter(anyString(), captor.capture());
            verifyNoMoreInteractions(psContainer);

            assertThat(captor.getAllValues()).extracting(PropertySource::getName).contains("test/first.yaml", "test/second.properties", "test/third.json");
        }

        @Test
        public void reloadWithExceptions() throws IOException {
            Files.createFile(testDirectory.resolve("a_third.json")); // empty file will cause an exception
            Files.createFile(testDirectory.resolve("b_second.properties"));
            Files.createFile(testDirectory.resolve("c_first.yaml"));

            when(psContainer.stream()).thenReturn(Stream.empty());

            source.reload(psContainer);

            ArgumentCaptor<PropertySource> captor = ArgumentCaptor.forClass(PropertySource.class);
            verify(psContainer, times(2)).addAfter(anyString(), captor.capture());
            verifyNoMoreInteractions(psContainer);

            assertThat(captor.getAllValues()).extracting(PropertySource::getName).contains("test/c_first.yaml", "test/b_second.properties");
        }
    }
}