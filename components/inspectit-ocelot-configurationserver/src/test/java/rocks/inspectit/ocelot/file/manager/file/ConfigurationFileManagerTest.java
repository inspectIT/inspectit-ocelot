package rocks.inspectit.ocelot.file.manager.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.manager.directory.GitDirectoryManager;
import rocks.inspectit.ocelot.file.manager.directory.WorkingDirectoryManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationFileManagerTest {

    @InjectMocks
    ConfigurationFileManager manager;

    @Mock
    GitDirectoryManager gitDirectoryManager;

    @Mock
    WorkingDirectoryManager workingDirectoryManager;

    @Nested
    public class ResolvePath {

        @Test
        public void resolvePath() {
            String result = manager.resolvePath("test");

            String expected = AbstractFileManager.FILES_DIRECTORY + File.separator + AbstractFileManager.CONFIG_DIRECTORY + File.separator + "test";
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    public class ReadFile {

        @Test
        public void readFile_wd() throws IOException {
            String resolvedPath = manager.resolvePath("test");
            when(workingDirectoryManager.readFile(resolvedPath)).thenReturn("content");

            String result = manager.readFile("test", false);

            assertThat(result).isEqualTo("content");
            verify(workingDirectoryManager).readFile(resolvedPath);
            verifyNoMoreInteractions(workingDirectoryManager);
            verifyZeroInteractions(gitDirectoryManager);
        }

        @Test
        public void readFile_git() throws IOException {
            when(gitDirectoryManager.readFile("test")).thenReturn("content");

            String result = manager.readFile("test", true);

            assertThat(result).isEqualTo("content");
            verify(gitDirectoryManager).readFile("test");
            verifyNoMoreInteractions(gitDirectoryManager);
            verifyZeroInteractions(workingDirectoryManager);
        }

        @Test
        public void exceptionWhileReading() throws IOException {
            when(workingDirectoryManager.readFile(anyString())).thenThrow(IOException.class);

            String result = manager.readFile("test", false);

            assertThat(result).isNull();
        }
    }

    @Nested
    public class ListFiles {

        @Test
        public void listFile_wd() throws IOException {
            List<FileInfo> dummyList = new ArrayList<>();
            String resolvedPath = manager.resolvePath("test");
            when(workingDirectoryManager.listFiles(resolvedPath, false)).thenReturn(dummyList);

            List<FileInfo> result = manager.listFiles("test", false, false);

            assertThat(result).isSameAs(dummyList);
            verify(workingDirectoryManager).listFiles(resolvedPath, false);
            verifyNoMoreInteractions(workingDirectoryManager);
            verifyZeroInteractions(gitDirectoryManager);
        }

        @Test
        public void readFile_git() throws IOException {
            List<FileInfo> dummyList = new ArrayList<>();
            when(gitDirectoryManager.listFiles("test", false)).thenReturn(dummyList);

            List<FileInfo> result = manager.listFiles("test", false, true);

            assertThat(result).isSameAs(dummyList);
            verify(gitDirectoryManager).listFiles("test", false);
            verifyNoMoreInteractions(gitDirectoryManager);
            verifyZeroInteractions(workingDirectoryManager);
        }
    }
}