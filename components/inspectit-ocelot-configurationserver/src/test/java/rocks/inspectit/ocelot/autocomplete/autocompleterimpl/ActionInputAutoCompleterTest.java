package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;

import java.util.Arrays;
import java.util.Collections;
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
        public void optionDataInput() {
            List<String> testActionPath1 = Arrays.asList("inspectit", "instrumentation", "rules", "constant-input", "preEntry", "*", "action");
            List<String> testActionPath2 = Arrays.asList("inspectit", "instrumentation", "rules", "constant-input", "preEntry", "*", "action");
            List<String> expected = Arrays.asList("test1");
            List<String> listOutPut1 = Collections.singletonList("test");
            List<String> listOutPut2 = Collections.singletonList("test");
            doReturn(listOutPut).when(yamlFileHelper).extractKeysFromYamlFiles(any());

            Mockito.when(yamlFileHelper.extractKeysFromYamlFiles(Mockito.any(List.class)))
                    .thenAnswer(new Answer() {
                        @Override
                        public Object answer(InvocationOnMock invocation) {
                            Object[] args = invocation.getArguments();
                            Object mock = invocation.getMock();
                            if (args[1].equals(testActionPath1)) {
                                return listOutPut;
                            } else if (args[1].equals(testActionPath2)) {
                                return
                            }

                        }
                    });

            List<String> output = actionInputAutoCompleter.getInput();

            assertThat(output).isEqualTo(expected);
        }

        @Test
        public void optionConstantInput() {
            List<String> testActionPath = Arrays.asList("inspectit", "instrumentation", "rules", "constant-input", "preEntry", "*", "action");
            List<String> expected = Arrays.asList("test");
            List<String> listOutPut = Collections.singletonList("test");
            when(yamlFileHelper.extractKeysFromYamlFiles(testActionPath)).thenReturn(listOutPut);

            List<String> output = actionInputAutoCompleter.getInput();

            assertThat(output).isEqualTo(expected);
        }
    }

    @Nested
    public class GetSuggestions {
        @Test
        public void getSuggestionsTest() {
            List<String> expected = Arrays.asList("test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test");
            List<String> outputYamlFileHelper = Arrays.asList("test");
            List<String> mockPath = Arrays.asList("inspectit", "instrumentation", "actions", "*");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(outputYamlFileHelper);

            List<String> output = actionInputAutoCompleter.getSuggestions(mockPath);

            assertThat(output).isEqualTo(expected);
        }

        @Test
        public void wrongPath() {
            List<String> expected = Arrays.asList();
            List<String> mockPath = Arrays.asList("inspectit", "instrumentation", "rules");

            List<String> output = actionInputAutoCompleter.getSuggestions(mockPath);

            assertThat(output).isEqualTo(expected);
        }
    }

}
