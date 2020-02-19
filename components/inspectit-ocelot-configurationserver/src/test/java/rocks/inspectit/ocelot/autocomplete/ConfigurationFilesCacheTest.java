package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationFilesCacheTest {

    @InjectMocks
    ConfigurationFilesCache configurationFilesCache;

    @Mock
    FileManager fileManager;

    @Nested
    public class GetAllPaths {
        @Test
        public void getYamlPaths() throws IOException {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.yml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            FileInfo mockFileInfo2 = Mockito.mock(FileInfo.class);
            Stream<String> streamB = Stream.of("path/b.yaml");
            when(mockFileInfo2.getAbsoluteFilePaths(any())).thenReturn(streamB);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo, mockFileInfo2);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);

            List<String> paths = configurationFilesCache.getAllPaths();

            assertThat(paths.size()).isEqualTo(2);
            assertThat(paths.contains("path/a.yml")).isTrue();
            assertThat(paths.contains("path/b.yaml")).isTrue();
        }
    }

    @Nested
    public class GetYamlContents {
        @Test
        public void testYamlLoadingMap() throws IOException {
            String yamlContent1 = "i am a:\n        - test\n        - yaml";
            String yamlContent2 = "so:\n    am: i";
            when(fileManager.readFile(any())).thenReturn(yamlContent1, yamlContent2);
            ConfigurationFilesCache spyConfigurationFilesCache = spy(configurationFilesCache);
            doReturn(Arrays.asList("a.yml", "b.yaml")).when(spyConfigurationFilesCache).getAllPaths();
            List<String> list = Arrays.asList("test", "yaml");
            LinkedHashMap<String, Object> firstElement = new LinkedHashMap<>();
            firstElement.put("i am a", list);
            LinkedHashMap<String, Object> secondElement = new LinkedHashMap<>();
            LinkedHashMap<String, Object> map2 = new LinkedHashMap<>();
            map2.put("am", "i");
            secondElement.put("so", map2);

            Collection<Object> beforeLoading = spyConfigurationFilesCache.getParsedConfigurationFiles();
            spyConfigurationFilesCache.loadFiles();
            Collection<Object> output = spyConfigurationFilesCache.getParsedConfigurationFiles();

            assertThat(output).isNotEqualTo(beforeLoading);
            assertThat(output.contains(firstElement)).isTrue();
            assertThat(output.contains(secondElement)).isTrue();
        }

        @Test
        public void errorOnLoading() throws IOException {
            ConfigurationFilesCache spyConfigurationFilesCache = spy(configurationFilesCache);
            doReturn(Collections.emptyList(), Arrays.asList("path/a.yml")).when(spyConfigurationFilesCache).getAllPaths();
            when(fileManager.readFile("path/a.yml")).thenThrow(IOException.class);

            spyConfigurationFilesCache.loadFiles();
            Collection<Object> before = spyConfigurationFilesCache.getParsedConfigurationFiles();
            spyConfigurationFilesCache.loadFiles();
            Collection<Object> output = spyConfigurationFilesCache.getParsedConfigurationFiles();

            assertThat(before).isEqualTo(output);
        }
    }
}

