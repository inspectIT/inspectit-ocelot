package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
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
            when(fileManager.getFilesInDirectory(null, true)).thenReturn(mockInfoList);

            List<String> paths = yamlLoader.getAllPaths();

            assertThat(paths.size()).isEqualTo(2);
            assertThat(paths.contains("path/a.yml")).isTrue();
            assertThat(paths.contains("path/b.yaml")).isTrue();
        }
    }

    @Nested
    public class GetYamlContents {
        @Test
        public void testYamlLoadingMap() throws IOException {
            YamlLoader yamlLoaderSpy = spy(yamlLoader);
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> stream = Stream.of("path/a.yml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(stream);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo);
            when(fileManager.getFilesInDirectory(null, true)).thenReturn(mockInfoList);
            Yaml mockYaml = Mockito.mock(Yaml.class);
            when(yamlLoaderSpy.getYaml()).thenReturn(mockYaml);
            when(fileManager.readFile(any())).thenReturn("path/a.yml");
            HashMap<String, String> map = new HashMap<>();
            map.put("path/a.yml", "i am a mock!");
            when(mockYaml.load("path/a.yml")).thenReturn(map);

            yamlLoaderSpy.loadFiles();
            Collection<Object> output = yamlLoaderSpy.getYamlContents();

            assertThat(output.contains("i am a mock!")).isTrue();
        }

        @Test
        public void testYamlLoadingList() throws IOException {
            YamlLoader yamlLoaderSpy = spy(yamlLoader);
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> stream = Stream.of("path/a.yml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(stream);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo);
            when(fileManager.getFilesInDirectory(null, true)).thenReturn(mockInfoList);
            Yaml mockYaml = Mockito.mock(Yaml.class);
            when(yamlLoaderSpy.getYaml()).thenReturn(mockYaml);
            when(fileManager.readFile(any())).thenReturn("path/a.yml");
            List<String> list = new ArrayList<>();
            list.add("i am a mock!");
            when(mockYaml.load("path/a.yml")).thenReturn(list);

            yamlLoaderSpy.loadFiles();
            Collection<Object> output = yamlLoaderSpy.getYamlContents();

            assertThat(output.contains("i am a mock!")).isTrue();
        }
    }
}

