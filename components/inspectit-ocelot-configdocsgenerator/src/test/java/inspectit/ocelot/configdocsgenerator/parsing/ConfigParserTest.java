package inspectit.ocelot.configdocsgenerator.parsing;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
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
import java.util.NoSuchElementException;

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
    class ParseConfigTests {

        @Test
        void withPlaceholder() throws IOException {

            String configYaml = getYaml("configWithPlaceholder.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            MetricDefinitionSettings metricDefinitionMock = Mockito.mock(MetricDefinitionSettings.class);
            when(metricDefinitionMock.isEnabled()).thenReturn(true);
            when(metricDefinitionMock.getType()).thenReturn(MetricDefinitionSettings.MeasureType.LONG);
            when(metricDefinitionMock.getUnit()).thenReturn("bytes");
            when(metricDefinitionMock.getMaxValuesPerTag()).thenReturn(5);
            when(metricDefinitionMock.getDescription()).thenReturn("free disk space");
            when(metricDefinitionMock.getViews()).thenReturn(null);

            assertThat(result.getMetrics().getDefinitions().get("disk/free")).usingRecursiveComparison()
                    .isEqualTo(metricDefinitionMock);
          // Build the expected result by hand
            Map<String, Boolean> enabledMap = new HashMap<>();
            enabledMap.put("free", true);
            StandardPollingMetricsRecorderSettings pollingSettingsMock = new StandardPollingMetricsRecorderSettings();
            pollingSettingsMock.setEnabled(enabledMap);
            assertThat(result.getMetrics().getDisk()).usingRecursiveComparison().isEqualTo(pollingSettingsMock);
        }

        @Test
        void withAdditionalInvalidRootKey() throws IOException {
            String configYaml = getYaml("configWithAdditionalInvalidRootKey.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            assertThat(result.getServiceName()).isEqualTo("name");
        }

        @Test
        void withOldExporterEnabled() throws IOException {
            String configYaml = getYaml("configWithOldExporterEnabled.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            // value 'true' in yaml to IF_CONFIGURED in InspectitConfig
            assertThat(result.getExporters()
                    .getMetrics()
                    .getInflux()
                    .getEnabled()).isEqualTo(ExporterEnabledState.IF_CONFIGURED);
            // value 'false' in yaml to DISABLED in InspectitConfig
            assertThat(result.getExporters()
                    .getTracing()
                    .getJaeger()
                    .getEnabled()).isEqualTo(ExporterEnabledState.DISABLED);
            // example for new style, value 'ENABLED' in yaml to ENABLED in InspectitConfig
            assertThat(result.getExporters()
                    .getMetrics()
                    .getPrometheus()
                    .getEnabled()).isEqualTo(ExporterEnabledState.ENABLED);
        }

        @Test
        void withUnknownProperty() throws IOException {
            String configYaml = getYaml("configWithUnknownProperty.yml");

            InspectitConfig result = configParser.parseConfig(configYaml);

            assertThat(result.getServiceName()).isEqualTo("name");
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

            assertThat(assertThatThrownBy(() -> configParser.parseConfig(configYaml)).isInstanceOf(NoSuchElementException.class));
        }
    }
}
