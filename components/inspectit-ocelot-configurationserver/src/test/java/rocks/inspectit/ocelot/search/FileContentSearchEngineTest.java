package rocks.inspectit.ocelot.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationFilesCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
            ArrayList<MatchedSubstringIndicator> filesList = new ArrayList<>();
            MatchedSubstringIndicator matchedSubstring1 = new MatchedSubstringIndicator();
            matchedSubstring1.setStart(0, 0);
            matchedSubstring1.setEnd(0, 4);
            MatchedSubstringIndicator matchedSubstring2 = new MatchedSubstringIndicator();
            matchedSubstring2.setStart(2, 4);
            matchedSubstring2.setEnd(2, 8);
            filesList.add(matchedSubstring1);
            filesList.add(matchedSubstring2);

            Map<?, ?> output = fileContentSearchEngine.searchInFiles("test1");

            assertThat(output.get("file")).isEqualTo(filesList);
        }

        @Test
        void findMultipleStringsInLine() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "testtesttest");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);
            ArrayList<MatchedSubstringIndicator> filesList = new ArrayList<>();
            MatchedSubstringIndicator matchedSubstring1 = new MatchedSubstringIndicator();
            matchedSubstring1.setStart(0, 0);
            matchedSubstring1.setEnd(0, 3);
            MatchedSubstringIndicator matchedSubstring2 = new MatchedSubstringIndicator();
            matchedSubstring2.setStart(0, 4);
            matchedSubstring2.setEnd(0, 7);
            MatchedSubstringIndicator matchedSubstring3 = new MatchedSubstringIndicator();
            matchedSubstring3.setStart(0, 8);
            matchedSubstring3.setEnd(0, 11);
            filesList.add(matchedSubstring1);
            filesList.add(matchedSubstring2);
            filesList.add(matchedSubstring3);

            Map<?, ?> output = fileContentSearchEngine.searchInFiles("test");

            assertThat(output.get("file")).isEqualTo(filesList);
        }

        @Test
        void queryOverLine() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file", "foo\nbar");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);
            ArrayList<MatchedSubstringIndicator> filesList = new ArrayList<>();
            MatchedSubstringIndicator matchedSubstring = new MatchedSubstringIndicator();
            matchedSubstring.setStart(0, 0);
            matchedSubstring.setEnd(1, 3);
            filesList.add(matchedSubstring);

            Map<?, ?> output = fileContentSearchEngine.searchInFiles("foo\nbar");

            assertThat(output.get("file")).isEqualTo(filesList);
        }

        @Test
        void stringNotPresent() {
            HashMap<String, String> testMap = new HashMap<>();
            testMap.put("file1", "test1 \n abc \n test1");
            when(configurationFilesCache.getFiles()).thenReturn(testMap);
            ArrayList<Integer> listFile = new ArrayList<>();
            listFile.add(1);
            listFile.add(3);

            Map<?, ?> output = fileContentSearchEngine.searchInFiles("foo");

            assertThat(output).isEmpty();
        }

        @Test
        void emptyQuery() {
            Map<?, ?> output = fileContentSearchEngine.searchInFiles("");

            assertThat(output).hasSize(0);
        }
    }
}