package inspectit.ocelot.configdocsgenerator.parsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.ActionDocumentation;
import rocks.inspectit.ocelot.config.model.metrics.StandardPollingMetricsRecorderSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigParserTest {

    private final ConfigParser configParser = new ConfigParser();

    /**
     * Helper method to read Yaml from resources.
     *
     * @param fileName Name of yml File in resources.
     *
     * @return String containing the Yaml from the yml file.
     */
    private String getYaml(String fileName) throws IOException {
        URL url = Resources.getResource("ConfigParserTest/" + fileName);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }

    @Nested
    class ReplacePlaceholdersTest {

        @Test
        void replacePlaceholders() throws IOException {

            String configYaml = getYaml("placeholdersOrigin.yml");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yamlMap = mapper.readValue(configYaml, Map.class);

            StringSubstitutor stringSubstitutor = new NestedMapStringSubstitutor(yamlMap);
            String result = stringSubstitutor.replace(configYaml);

            String expected = getYaml("placeholdersExpected.yml");

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class ParseConfigTests {

        @Test
        void withPlaceholder() throws IOException {

            String configYaml = getYaml("configWithPlaceholder.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            // Build the expected result by hand
            Map<String, Boolean> enabledMap = new HashMap<>();
            enabledMap.put("free", true);
            StandardPollingMetricsRecorderSettings pollingSettingsMock = Mockito.mock(StandardPollingMetricsRecorderSettings.class);
            when(pollingSettingsMock.getEnabled()).thenReturn(enabledMap);

            MetricDefinitionSettings metricDefinitionMock = Mockito.mock(MetricDefinitionSettings.class);
            when(metricDefinitionMock.isEnabled()).thenReturn(true);
            when(metricDefinitionMock.getType()).thenReturn(MetricDefinitionSettings.MeasureType.LONG);
            when(metricDefinitionMock.getUnit()).thenReturn("bytes");
            when(metricDefinitionMock.getDescription()).thenReturn("free disk space");
            when(metricDefinitionMock.getViews()).thenReturn(null);

            assertThat(result.getMetrics().getDefinitions().get("[disk/free]")).usingRecursiveComparison()
                    .isEqualTo(metricDefinitionMock);
            assertThat(result.getMetrics().getDisk()).usingRecursiveComparison().isEqualTo(pollingSettingsMock);
        }

        @Test
        void withDuration() throws IOException {

            String configYaml = getYaml("configWithDuration.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            assertThat(result.getInstrumentation().getInternal().getInterBatchDelay()).isEqualTo(Duration.ofMillis(50));
            assertThat(result.getInstrumentation()
                    .getInternal()
                    .getNewClassDiscoveryInterval()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        void withDocumentation() throws IOException {

            String configYaml = getYaml("configWithDocumentation.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            // Build the expected result by hand
            Map<String, String> inputDescs = new HashMap<>();
            inputDescs.put("value", "Object to be printed");

            ActionDocumentation actionDocSettingsMock = Mockito.mock(ActionDocumentation.class);
            when(actionDocSettingsMock.getDescription()).thenReturn("Prints a given Object to stdout.");
            when(actionDocSettingsMock.getSince()).thenReturn("1.0");
            when(actionDocSettingsMock.getInputs()).thenReturn(inputDescs);
            when(actionDocSettingsMock.getReturnValue()).thenReturn("Void");

            Map<String, String> inputs = new HashMap<>();
            inputs.put("value", "Object");
            GenericActionSettings actionSettingsMock = Mockito.mock(GenericActionSettings.class);
            when(actionSettingsMock.getDocs()).thenReturn(actionDocSettingsMock);
            when(actionSettingsMock.getInput()).thenReturn(inputs);
            when(actionSettingsMock.getIsVoid()).thenReturn(true);
            when(actionSettingsMock.getValueBody()).thenReturn("System.out.println(value);");

            assertThat(result.getInstrumentation().getActions().get("a_debug_println")).usingRecursiveComparison()
                    .isEqualTo(actionSettingsMock);
        }

        @Test
        void invalidYaml() {
            String configYaml = "invalid-yaml";

            assertThat(assertThatThrownBy(() -> configParser.parseConfig(configYaml)).isInstanceOf(JsonProcessingException.class));
        }
    }
}