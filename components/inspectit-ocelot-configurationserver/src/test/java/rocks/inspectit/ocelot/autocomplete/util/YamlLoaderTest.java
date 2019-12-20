package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YamlLoaderTest {

    @InjectMocks
    YamlLoader yamlLoader;

    @Mock
    FileManager fileManager;

    @Nested
    public class LoadYaml {
        @Test
        public void testLoadYaml() throws IOException {
            String testPath = "mockPath";
            String mockObject = "mock";
            when(fileManager.readFile(any())).thenReturn(mockObject);

            assertThat(yamlLoader.loadYaml(testPath)).isEqualTo(mockObject);
        }

        @Test
        public void fileManagerReturnsNull() throws IOException {
            String testPath = "mockPath";
            when(fileManager.readFile(any())).thenReturn(null);

            assertThat(yamlLoader.loadYaml(testPath)).isEqualTo(null);
        }

    }
}

