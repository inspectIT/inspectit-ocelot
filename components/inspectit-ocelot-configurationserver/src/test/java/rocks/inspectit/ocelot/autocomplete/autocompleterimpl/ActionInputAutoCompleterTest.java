package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ActionInputAutoCompleterTest {

    @InjectMocks
    ActionInputAutoCompleter actionInputAutoCompleter;

    @Mock
    YamlFileHelper yamlFileHelper;


    @Nested
    public class GetInput {
        @Test
        public void getInputTest() {
            List<String> output = Arrays.asList("test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test");
            List<String> outputYamlFileHelper = Arrays.asList("test");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(outputYamlFileHelper);

            assertThat(actionInputAutoCompleter.getInput()).isEqualTo(output);
        }
    }

    @Nested
    public class GetSuggestions {
        @Test
        public void getSuggestionsTest() {
            List<String> output = Arrays.asList("test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test");
            List<String> outputYamlFileHelper = Arrays.asList("test");
            List<String> mockPath = Arrays.asList("inspectit", "instrumentation", "actions", "*");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(outputYamlFileHelper);

            assertThat(actionInputAutoCompleter.getSuggestions(mockPath)).isEqualTo(output);
        }

        @Test
        public void wrongPath() {
            List<String> output = Arrays.asList();
            List<String> mockPath = Arrays.asList("inspectit", "instrumentation", "rules");

            assertThat(actionInputAutoCompleter.getSuggestions(mockPath)).isEqualTo(output);
        }
    }

}
