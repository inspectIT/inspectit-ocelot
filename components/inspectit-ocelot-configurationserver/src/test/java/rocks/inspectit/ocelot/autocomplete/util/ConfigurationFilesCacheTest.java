package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationFilesCacheTest {

    @InjectMocks
    ConfigurationFilesCache configurationFilesCache;

    @Mock
    FileManager fileManager;

    @Mock
    WorkingDirectoryAccessor workingDirectoryAccessor;

    @BeforeEach
    public void beforeEach() {
        lenient().when(fileManager.getWorkingDirectory()).thenReturn(workingDirectoryAccessor);
    }

    @Nested
    public class LoadYamlFile {
        @Test
        public void testLoadYaml() {
            String testPath = "mockPath";
            String yamlContent = "i am a:\n        - test\n        - yaml";
            when(workingDirectoryAccessor.readConfigurationFile(any())).thenReturn(Optional.of(yamlContent));

            LinkedHashMap<Object, Object> output = (LinkedHashMap<Object, Object>) configurationFilesCache.loadYamlFile(testPath);

            assertThat(output).hasSize(1)
                    .containsEntry("i am a", Arrays.asList("test", "yaml"));
        }

        @Test
        public void fileManagerReturnsNull() throws IOException {
            String testPath = "mockPath";
            when(workingDirectoryAccessor.readConfigurationFile(any())).thenReturn(Optional.empty());

            Object output = configurationFilesCache.loadYamlFile(testPath);

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
            when(workingDirectoryAccessor.listConfigurationFiles("")).thenReturn(Optional.of(mockInfoList));

            List<String> paths = configurationFilesCache.getAllPaths();

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
            when(workingDirectoryAccessor.listConfigurationFiles("")).thenReturn(Optional.of(mockInfoList));

            List<String> paths = configurationFilesCache.getAllPaths();

            assertThat(paths.size()).isEqualTo(0);
        }
    }

    @Nested
    public class LoadFiles {
        @Test
        public void testYamlLoadingMap() throws IOException {
            String yamlContent1 = "i am a:\n        - test\n        - yaml";
            String yamlContent2 = "so:\n    am: i";
            when(workingDirectoryAccessor.readConfigurationFile(any())).thenReturn(Optional.of(yamlContent1), Optional.of(yamlContent2));
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getAbsoluteFilePaths("")).thenReturn(Stream.of("a.yaml"));
            FileInfo mockFileInfo2 = mock(FileInfo.class);
            when(mockFileInfo2.getAbsoluteFilePaths("")).thenReturn(Stream.of("b.yaml"));
            when(workingDirectoryAccessor.listConfigurationFiles("")).thenReturn(Optional.of(Arrays.asList(mockFileInfo1, mockFileInfo2)));
            List<String> list = Arrays.asList("test", "yaml");
            LinkedHashMap<String, Object> firstElement = new LinkedHashMap<>();
            firstElement.put("i am a", list);
            LinkedHashMap<String, Object> secondElement = new LinkedHashMap<>();
            LinkedHashMap<String, Object> map2 = new LinkedHashMap<>();
            map2.put("am", "i");
            secondElement.put("so", map2);

            Collection<Object> beforeLoading = configurationFilesCache.getParsedConfigurationFiles();
            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(output).isNotEqualTo(beforeLoading);
            assertThat(output.contains(firstElement)).isTrue();
            assertThat(output.contains(secondElement)).isTrue();
        }

        @Test
        public void errorOnLoading() throws IOException {
            configurationFilesCache.loadFiles();
            Collection<Object> before = configurationFilesCache.getParsedConfigurationFiles();
            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(before).isEqualTo(output);
        }
    }
}

