package inspectit.ocelot.configdocsgenerator.parsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigParserTest {

    private final ConfigParser configParser = new ConfigParser();

    @Nested
    class ReplacePlaceholdersTest {

        @Test
        void replacePlaceholders() throws JsonProcessingException {

            final String variable1 = "inspectit.name.placeholder-value";
            final String variable2 = "inspectit.name.second.placeholder-value";
            final String variable3 = "inspectit.doesnotexist";
            final String variable4 = "doesnotexist";
            final String variable5 = "inspectit.name.dot.placeholder-value";

            final String configYaml = String.format(
                    "inspectit:\n" +
                    // Test replacement of placeholders at different deepness levels
                    "  placeholder-test: ${%s}\n" +
                    "  placeholder-test2: ${%s}\n" +
                    // Test replacement if placeholder-keys do not lead to a value
                    "  placeholder-test3: ${%s}\n" +
                    // Test replacement with key containing dots
                    "  placeholder-test5: ${%s}\n" +
                    "  name:\n" +
                    "    placeholder-value: value\n" +
                    "    dot.placeholder-value: value5\n" +
                    "    second:\n" +
                    "      placeholder-value: value2\n" +
                    "      placeholder-test4: ${%s}",
                    variable1, variable2, variable3, variable5, variable4);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yamlMap = mapper.readValue(configYaml, Map.class);

            StringSubstitutor stringSubstitutor = new StringSubstitutorNestedMap(yamlMap);
            String result = stringSubstitutor.replace(configYaml);

            final String expected = String.format(
                    "inspectit:\n" +
                    "  placeholder-test: value\n" +
                    "  placeholder-test2: value2\n" +
                    "  placeholder-test3: ${%s}\n" +
                    "  placeholder-test5: value5\n" +
                    "  name:\n" +
                    "    placeholder-value: value\n" +
                    "    dot.placeholder-value: value5\n" +
                    "    second:\n" +
                    "      placeholder-value: value2\n" +
                    "      placeholder-test4: ${%s}",
                    variable3, variable4);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class ParseConfigTests {

        @Test
        void withPlaceholder() {

            final String metricName = "[disk/free]";

            final String configYaml = String.format(
                    "inspectit:\n" +
                    "  metrics:\n" +
                    "    disk:\n" +
                    "      enabled:\n" +
                    "        # if true, the free disk space will be measured and the view \"disk/free\" is registered\n" +
                    "        free: true\n" +
                    "    definitions:\n" +
                    "      '%s':\n" +
                    "        enabled: ${inspectit.metrics.disk.enabled.free}\n" +
                    "        type: LONG\n" +
                    "        unit: bytes\n" +
                    "        description: free disk space"
                    , metricName);

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

            assertThat(result.getMetrics().getDefinitions().get(metricName)).usingRecursiveComparison()
                    .isEqualTo(metricDefinitionMock);
            assertThat(result.getMetrics().getDisk()).usingRecursiveComparison().isEqualTo(pollingSettingsMock);
        }

        @Test
        void withDuration() {

            final String configYaml =
                "inspectit:\n" +
                "  instrumentation:\n" +
                "    internal:\n" +
                "      inter-batch-delay: 50ms\n" +
                "      new-class-discovery-interval: 10s";

            InspectitConfig result = configParser.parseConfig(configYaml);

            assertThat(result.getInstrumentation().getInternal().getInterBatchDelay()).isEqualTo(Duration.ofMillis(50));
            assertThat(result.getInstrumentation()
                    .getInternal()
                    .getNewClassDiscoveryInterval()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        void withDocumentation() {

            final String configYaml =
                "inspectit:\n" +
                "  instrumentation:\n" +
                "    actions:\n" +
                "      a_debug_println:\n" +
                "        docs:\n" +
                "          description: 'Prints a given Object to stdout.'\n" +
                "          since: '1.0'\n" +
                "          inputs:\n" +
                "            value: Object to be printed\n" +
                "          return-value: Void\n" +
                "        input:\n" +
                "          value: Object\n" +
                "        is-void: true\n" +
                "        value-body: |\n" +
                "          System.out.println(value);";

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
    }
}