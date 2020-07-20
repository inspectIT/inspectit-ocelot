package rocks.inspectit.ocelot.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileContentSearchEngineTest {

    @InjectMocks
    FileContentSearchEngine fileContentSearchEngine;

    @Mock
    FileManager fileManager;

    @Nested
    public class searchLines {

        @Test
        void findSingleStringPerLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            FileInfo mockFileInfo2 = mock(FileInfo.class);
            when(mockFileInfo1.getName()).thenReturn("file_test1");
            when(mockFileInfo2.getName()).thenReturn("file_test2");
            List<FileInfo> testFiles = Arrays.asList(mockFileInfo1, mockFileInfo2);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            doReturn(java.util.Optional.of("i am the test1 content"), java.util.Optional.of("i am the test2 content")).when(mockAccess)
                    .readConfigurationFile(any());

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test1", -1);

            assertThat(output).hasSize(1);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(0);
            assertThat(result.getStartColumn()).isEqualTo(9);
            assertThat(result.getEndColumn()).isEqualTo(14);
        }

        @Test
        void findMultipleStringsInLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getName()).thenReturn("file_test1");
            List<FileInfo> testFiles = Collections.singletonList(mockFileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("test1test1test1"));

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test1", -1);

            assertThat(output).hasSize(3);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(0);
            assertThat(result.getStartColumn()).isEqualTo(0);
            assertThat(result.getEndColumn()).isEqualTo(5);
            result = output.get(1);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(0);
            assertThat(result.getStartColumn()).isEqualTo(5);
            assertThat(result.getEndColumn()).isEqualTo(10);
            result = output.get(2);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(0);
            assertThat(result.getStartColumn()).isEqualTo(10);
            assertThat(result.getEndColumn()).isEqualTo(15);
        }

        @Test
        void queryOverLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getName()).thenReturn("file_test1");
            List<FileInfo> testFiles = Collections.singletonList(mockFileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("foo\nbar"));

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("foo\nbar", -1);

            assertThat(output).hasSize(1);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(0);
            assertThat(result.getEndColumn()).isEqualTo(3);

        }

        @Test
        void withLimit() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getName()).thenReturn("file_test1");
            List<FileInfo> testFiles = Collections.singletonList(mockFileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("testtesttest"));

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test", 1);

            assertThat(output).hasSize(1);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file_test1");
            assertThat(result.getStartLine()).isEqualTo(0);
            assertThat(result.getEndLine()).isEqualTo(0);
            assertThat(result.getStartColumn()).isEqualTo(0);
            assertThat(result.getEndColumn()).isEqualTo(4);
        }

        @Test
        void stringNotPresent() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getName()).thenReturn("file_test1");
            List<FileInfo> testFiles = Collections.singletonList(mockFileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("test1 \n abc \n test1"));

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("foo", -1);

            assertThat(output).isEmpty();
        }

        @Test
        void emptyQuery() {
            List<SearchResult> output = fileContentSearchEngine.searchInFiles("", -1);

            assertThat(output).hasSize(0);
        }
    }

}
