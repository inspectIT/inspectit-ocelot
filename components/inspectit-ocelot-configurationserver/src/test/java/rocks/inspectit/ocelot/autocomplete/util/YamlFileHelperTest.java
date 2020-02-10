package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YamlFileHelperTest {
    @Mock
    private YamlLoader yamlLoader;

    @InjectMocks
    private YamlFileHelper autoCompleter;

    @Nested
    public class GetSuggestions {

        @Test
        public void getScopeSuggestion() {
            ArrayList<String> propertyPath = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes"));
            ArrayList<String> output = new ArrayList<>(Arrays.asList("my_scope1", "another_scope1"));
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> instrumentation = new HashMap<>();
            HashMap<String, Object> scopes = new HashMap<>();
            scopes.put("my_scope1", null);
            scopes.put("another_scope1", null);
            instrumentation.put("scopes", scopes);
            inspectit.put("instrumentation", instrumentation);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void getNonScopeSuggestion() {
            List<String> propertyPath = Arrays.asList("inspectit", "metrics");
            List<String> output = Arrays.asList("definitions");
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> metrics = new HashMap<>();
            HashMap<String, Object> definitions = new HashMap<>();
            definitions.put("def1", null);
            definitions.put("def2", null);
            metrics.put("definitions", definitions);
            inspectit.put("metrics", metrics);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void noScopeDefined() {
            ArrayList<String> propertyPath = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes"));
            ArrayList<String> output = new ArrayList<>();
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            HashMap<String, Object> metrics = new HashMap<>();
            HashMap<String, Object> definitions = new HashMap<>();
            definitions.put("def1", null);
            definitions.put("def2", null);
            metrics.put("definitions", definitions);
            inspectit.put("metrics", metrics);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void withWildCard() {
            List<String> propertyPath = Arrays.asList("inspectit", "*");
            List<String> output = Arrays.asList("definitions", "rules", "somethingElse", "listContent1", "listContent2");
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
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
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void throughList() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "0");
            List<String> output = Arrays.asList("Hello!");
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void wildcardThroughList() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "*");
            List<String> output = Arrays.asList("Hello", "there!");
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void nonNumberListIndex() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "iShouldNotBeHere");
            List<String> output = new ArrayList<>();
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void indexTooSmall() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "-5");
            List<String> output = new ArrayList<>();
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }

        @Test
        public void indexTooBig() {
            List<String> propertyPath = Arrays.asList("inspectit", "exampleList", "50000");
            List<String> output = new ArrayList<>();
            YamlFileHelper autoCompleter1 = Mockito.spy(autoCompleter);
            HashMap<String, Object> topLevelMap = new HashMap<>();
            HashMap<String, Object> inspectit = new HashMap<>();
            List<String> list = Arrays.asList("Hello", "there!");
            inspectit.put("exampleList", list);
            topLevelMap.put("inspectit", inspectit);
            Collection<Object> mockData = Arrays.asList(topLevelMap);
            when(yamlLoader.getYamlContents()).thenReturn(mockData);

            assertThat(autoCompleter1.extractKeysFromYamlFiles(propertyPath)).isEqualTo(output);
        }
    }
}
