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
public class ActionAutoCompleterTest {

    @InjectMocks
    ActionAutoCompleter actionAutoCompleter;

    @Mock
    YamlFileHelper yamlFileHelper;

    @Nested
    public class GetActions {
        @Test
        public void getActionsTest() {
            List<String> output = Arrays.asList("Hello", "Hello", "Hello", "Hello", "Hello", "Hello");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(Arrays.asList("Hello"));

            assertThat(actionAutoCompleter.getActions()).isEqualTo(output);
        }
    }

    @Nested
    public class GetSuggestions {
        @Test
        public void getSuggestionsTest() {
            List<String> output = Arrays.asList("test", "test", "test", "test", "test", "test");
            List<String> path = Arrays.asList("inspectit", "instrumentation", "actions");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(Arrays.asList("test"));

            assertThat(actionAutoCompleter.getSuggestions(path)).isEqualTo(output);
        }

        @Test
        public void wrongPath() {
            List<String> output = Arrays.asList();
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules");

            assertThat(actionAutoCompleter.getSuggestions(path)).isEqualTo(output);
        }

    }
}
