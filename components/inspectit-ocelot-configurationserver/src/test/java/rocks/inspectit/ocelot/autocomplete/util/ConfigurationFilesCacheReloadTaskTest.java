package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.error.YAMLException;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationFilesCacheReloadTaskTest {

    ConfigurationFilesCacheReloadTask reloadTask;

    @Mock
    AbstractFileAccessor fileAccessor;

    private Collection<Object> resultHolder;

    @BeforeEach
    public void setup() {
        reloadTask = new ConfigurationFilesCacheReloadTask(fileAccessor, (result) -> resultHolder = result);
    }

    @Nested
    public class LoadYamlFile {

        @Test
        public void testLoadYaml() {
            String testPath = "mockPath";
            String yamlContent = "i am a:\n        - test\n        - yaml";
            when(fileAccessor.readConfigurationFile(any())).thenReturn(Optional.of(yamlContent));

            Object output = reloadTask.loadYamlFile(testPath);

            assertThat(output).isInstanceOf(Map.class);
            assertThat((Map<Object, Object>) output).hasSize(1).containsEntry("i am a", Arrays.asList("test", "yaml"));
        }

        @Test
        public void fileManagerReturnsNull() {
            String testPath = "mockPath";
            when(fileAccessor.readConfigurationFile(any())).thenReturn(Optional.empty());

            Object output = reloadTask.loadYamlFile(testPath);

            assertThat(output).isEqualTo(null);
        }

        @Test
        public void exceptionOnYamlParsing() {
            String testPath = "mockPath";
            String yamlContent = "foo:bar";
            when(fileAccessor.readConfigurationFile(any())).thenReturn(Optional.of(yamlContent));
            ConfigurationFilesCacheReloadTask spyReloadTask = spy(reloadTask);
            when(spyReloadTask.parseYaml("mockPath")).thenThrow(new YAMLException("test"));

            Object output = spyReloadTask.loadYamlFile(testPath);

            assertThat(output).isEqualTo(null);
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
            when(fileAccessor.listConfigurationFiles("")).thenReturn(mockInfoList);

            List<String> paths = reloadTask.getAllPaths();

            assertThat(paths.size()).isEqualTo(2);
            assertThat(paths.contains("path/a.yml")).isTrue();
            assertThat(paths.contains("path/b.yaml")).isTrue();
        }

        @Test
        public void containsNonYamlFile() {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.xml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo);
            when(fileAccessor.listConfigurationFiles("")).thenReturn(mockInfoList);

            List<String> paths = reloadTask.getAllPaths();

            assertThat(paths.size()).isEqualTo(0);
        }
    }

    @Nested
    public class Run {

        @Test
        public void testYamlLoadingMap() throws IOException {
            String yamlContent1 = "i am a:\n        - test\n        - yaml";
            String yamlContent2 = "so:\n    am: i";
            when(fileAccessor.readConfigurationFile(any())).thenReturn(Optional.of(yamlContent1), Optional.of(yamlContent2));
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getAbsoluteFilePaths("")).thenReturn(Stream.of("a.yaml"));
            FileInfo mockFileInfo2 = mock(FileInfo.class);
            when(mockFileInfo2.getAbsoluteFilePaths("")).thenReturn(Stream.of("b.yaml"));
            when(fileAccessor.listConfigurationFiles("")).thenReturn(Arrays.asList(mockFileInfo1, mockFileInfo2));
            List<String> list = Arrays.asList("test", "yaml");
            LinkedHashMap<String, Object> firstElement = new LinkedHashMap<>();
            firstElement.put("i am a", list);
            LinkedHashMap<String, Object> secondElement = new LinkedHashMap<>();
            LinkedHashMap<String, Object> map2 = new LinkedHashMap<>();
            map2.put("am", "i");
            secondElement.put("so", map2);

            reloadTask.run();

            assertThat(resultHolder.contains(firstElement)).isTrue();
            assertThat(resultHolder.contains(secondElement)).isTrue();
        }
    }
}

