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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileContentSearchEngineTest {

    @InjectMocks
    FileContentSearchEngine searchEngine;

    @Mock
    FileManager fileManager;

    @Nested
    class SearchLines {

        @Test
        void findSingleStringPerLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo1 = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            FileInfo fileInfo2 = FileInfo.builder().name("file_test2").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Arrays.asList(fileInfo1, fileInfo2);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            doReturn(java.util.Optional.of("i am the test1 content"), java.util.Optional.of("i am the test2 content")).when(mockAccess)
                    .readConfigurationFile(any());

            List<SearchResult> output = searchEngine.search("test1", 100, false);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", null, 0, 9, 0, 14));
        }

        @Test
        void findMultipleStringsInLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("test1test1test1"));

            List<SearchResult> output = searchEngine.search("test1", 100, false);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", null, 0, 0, 0, 5), tuple("file_test1", null, 0, 5, 0, 10), tuple("file_test1", null, 0, 10, 0, 15));
        }

        @Test
        void queryOverLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("foo\nbar"));

            List<SearchResult> output = searchEngine.search("foo\nbar", 100, false);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", null, 0, 0, 1, 3));
        }

        @Test
        void withLimit() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("testtesttest"));

            List<SearchResult> output = searchEngine.search("test", 1, false);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", null, 0, 0, 0, 4));
        }

        @Test
        void stringNotPresent() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("test1 \n abc \n test1"));

            List<SearchResult> output = searchEngine.search("foo", 100, false);

            assertThat(output).isEmpty();
        }

        @Test
        void matchingStringNotInFirstLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            when(mockAccess.readConfigurationFile(any())).thenReturn(java.util.Optional.of("test2\ntest1\ntest2 next line\nand another one\nits here: test2"));

            List<SearchResult> output = searchEngine.search("test2", 100, false);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", null, 0, 0, 0, 5), tuple("file_test1", null, 2, 0, 2, 5), tuple("file_test1", null, 4, 10, 4, 15));
        }

        @Test
        void emptyQuery() {
            List<SearchResult> output = searchEngine.search("", 100, false);

            assertThat(output).isEmpty();
        }

        @Test
        void retrieveFromFirstLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo1 = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            doReturn(java.util.Optional.of("i am the test1 content")).when(mockAccess).readConfigurationFile(any());

            List<SearchResult> output = searchEngine.search("test1", 100, true);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", "i am the test1 content", 0, 9, 0, 14));
        }

        @Test
        void retrieveFromMiddleLine() {
            RevisionAccess mockAccess = mock(RevisionAccess.class);
            when(fileManager.getWorkspaceRevision()).thenReturn(mockAccess);
            FileInfo fileInfo1 = FileInfo.builder().name("file_test1").type(FileInfo.Type.FILE).build();
            List<FileInfo> testFiles = Collections.singletonList(fileInfo1);
            when(mockAccess.listConfigurationFiles("")).thenReturn(testFiles);
            doReturn(java.util.Optional.of("i \n am \n the test1 \n content")).when(mockAccess)
                    .readConfigurationFile(any());

            List<SearchResult> output = searchEngine.search("test1", 100, true);

            assertThat(output).extracting(SearchResult::getFile, SearchResult::getFirstLine, SearchResult::getStartLine, SearchResult::getStartColumn, SearchResult::getEndLine, SearchResult::getEndColumn)
                    .containsExactly(tuple("file_test1", " the test1 ", 2, 5, 2, 10));
        }
    }
}
