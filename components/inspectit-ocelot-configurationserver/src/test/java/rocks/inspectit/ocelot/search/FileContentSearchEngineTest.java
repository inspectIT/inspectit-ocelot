package rocks.inspectit.ocelot.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationFilesCache;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileContentSearchEngineTest {

    @InjectMocks
    FileContentSearchEngine fileContentSearchEngine;

    @Mock
    ConfigurationFilesCache configurationFilesCache;


    @Nested
    public class searchLines {

        @Test
        void findSingleStringPerLine() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "test1 \nabc \nfootest1");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test1", -1);

            assertThat(output).hasSize(2);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(1);
            assertThat(result.getEndColumn()).isEqualTo(5);
            result = output.get(1);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(3);
            assertThat(result.getEndLine()).isEqualTo(3);
            assertThat(result.getStartColumn()).isEqualTo(4);
            assertThat(result.getEndColumn()).isEqualTo(8);
        }

        @Test
        void findMultipleStringsInLine() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "testtesttest");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test", -1);

            assertThat(output).hasSize(3);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(1);
            assertThat(result.getEndColumn()).isEqualTo(4);
            result = output.get(1);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(5);
            assertThat(result.getEndColumn()).isEqualTo(8);
            result = output.get(2);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(9);
            assertThat(result.getEndColumn()).isEqualTo(12);
        }

        @Test
        void queryOverLine() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "foo\nbar");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("foo\nbar", -1);

            assertThat(output).hasSize(1);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(2);
            assertThat(result.getStartColumn()).isEqualTo(1);
            assertThat(result.getEndColumn()).isEqualTo(3);

        }

        @Test
        void withLimit() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "testtesttest");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);

            List<SearchResult> output = fileContentSearchEngine.searchInFiles("test", 1);

            assertThat(output).hasSize(1);
            SearchResult result = output.get(0);
            assertThat(result.getFile()).isEqualTo("file");
            assertThat(result.getStartLine()).isEqualTo(1);
            assertThat(result.getEndLine()).isEqualTo(1);
            assertThat(result.getStartColumn()).isEqualTo(1);
            assertThat(result.getEndColumn()).isEqualTo(4);
        }

        @Test
        void stringNotPresent() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file1", "test1 \n abc \n test1");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);

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