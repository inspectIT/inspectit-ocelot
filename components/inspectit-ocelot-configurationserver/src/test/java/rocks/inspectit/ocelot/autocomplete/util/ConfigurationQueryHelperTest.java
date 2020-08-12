package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigurationQueryHelperTest {

    @Mock
    private ConfigurationFilesCache configurationFilesCache;

    @InjectMocks
    private ConfigurationQueryHelper configurationQueryHelper;

    @Nested
    public class GetKeysForPath {

        @Test
        public void getScopeSuggestion() {
            List<String> propertyPath = Arrays.asList("inspectit", "instrumentation", "scopes");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> instrumentation = new HashMap<>();
            HashMap<String, Object> scopes = new HashMap<>();
            scopes.put("my_scope1", null);
            scopes.put("another_scope1", null);
            instrumentation.put("scopes", scopes);
            inspectit.put("instrumentation", instrumentation);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<String> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(2);
            assertThat(output).contains("my_scope1");
            assertThat(output).contains("another_scope1");
        }

        @Test
        public void getNonScopeSuggestion() {
            List<String> propertyPath = Arrays.asList("inspectit", "metrics");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> metrics = new HashMap<>();
            HashMap<String, Object> definitions = new HashMap<>();
            definitions.put("def1", null);
            definitions.put("def2", null);
            metrics.put("definitions", definitions);
            inspectit.put("metrics", metrics);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<String> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(1);
            assertThat(output).contains("definitions");
        }

        @Test
        public void noScopeDefined() {
            List<String> propertyPath = Arrays.asList("inspectit", "instrumentation", "scopes");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> metrics = new HashMap<>();
            HashMap<String, Object> definitions = new HashMap<>();
            definitions.put("def1", null);
            definitions.put("def2", null);
            metrics.put("definitions", definitions);
            inspectit.put("metrics", metrics);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<?> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(0);
        }

        @Test
        public void withWildCard() {
            List<String> propertyPath = Arrays.asList("inspectit", "*");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> instrumentation = new HashMap<>();
            HashMap<String, Object> metrics = new HashMap<>();
            HashMap<String, Object> definitions = new HashMap<>();
            List<String> list = Arrays.asList("listContent1", "listContent2");
            definitions.put("def1", null);
            definitions.put("def2", null);
            metrics.put("definitions", definitions);
            instrumentation.put("rules", null);
            instrumentation.put("somethingElse", null);
            inspectit.put("metrics", metrics);
            inspectit.put("instrumentation", instrumentation);
            inspectit.put("list", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<String> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(5);
            assertThat(output).contains("definitions");
            assertThat(output).contains("rules");
            assertThat(output).contains("somethingElse");
            assertThat(output).contains("listContent1");
            assertThat(output).contains("listContent2");
        }

        @Test
        public void throughList() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "0");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Collections.singletonList("Hello!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<String> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(1);
            assertThat(output).contains("Hello!");
        }

        @Test
        public void wildcardThroughList() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "*");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<String> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(2);
            assertThat(output).contains("Hello");
            assertThat(output).contains("there!");
        }

        @Test
        public void nonNumberListIndex() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "iShouldNotBeHere");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<?> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(0);
        }

        @Test
        public void indexTooSmall() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "-5");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<?> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(0);
        }

        @Test
        public void indexTooBig() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "50000");
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Collections.singletonList(topLevelMap);
            when(configurationFilesCache.getParsedContents()).thenReturn(mockData);

            List<?> output = configurationQueryHelper.getKeysForPath(propertyPath);

            assertThat(output).hasSize(0);
        }
    }
}
