package inspectit.ocelot.config.doc.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import parsing.ConfigParser;
import parsing.StringSubstitutorNestedMap;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.ActionDocSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.StandardPollingMetricsRecorderSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigParserTest {

    private final ConfigParser configParser = new ConfigParser();

    @Nested
    class ReplacePlaceholdersTest{

        @Test
        void replacePlaceholders() throws JsonProcessingException {

            final String variable1 = "inspectit.name.placeholder-value";
            final String variable2 = "inspectit.name.second.placeholder-value";
            final String variable3 = "inspectit.doesnotexist";
            final String variable4 = "doesnotexist";

            final String configYaml = String.format(
                    "inspectit:\n" +
                    // Test replacement of placeholders at different deepness levels
                    "  placeholder-test: ${%s}\n" +
                    "  placeholder-test2: ${%s}\n" +
                    // Test replacement if placeholder-keys do not lead to a value
                    "  placeholder-test3: ${%s}\n" +
                    "  name:\n" +
                    "    placeholder-value: value\n" +
                    "    second:\n" +
                    "      placeholder-value: value2\n" +
                    "      placeholder-test4: ${%s}",
                    variable1, variable2, variable3, variable4);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yamlMap = mapper.readValue(configYaml, Map.class);
            StringSubstitutor stringSubstitutor = new StringSubstitutorNestedMap(yamlMap);
            String result = stringSubstitutor.replace(configYaml);

            final String expected = String.format(
                    "inspectit:\n" +
                    "  placeholder-test: value\n" +
                    "  placeholder-test2: value2\n" +
                    "  placeholder-test3: ${%s}\n" +
                    "  name:\n" +
                    "    placeholder-value: value\n" +
                    "    second:\n" +
                    "      placeholder-value: value2\n" +
                    "      placeholder-test4: ${%s}",
                    variable3, variable4);

            assertEquals(expected, result);
        }
    }

    @Nested
    class ParseConfigTests{

        @Test
        void withPlaceholder() {

            final String configYaml =
                "inspectit:\n" +
                "  metrics:\n" +
                "    disk:\n" +
                "      enabled:\n" +
                "        # if true, the free disk space will be measured and the view \"disk/free\" is registered\n" +
                "        free: true\n" +
                "    definitions:\n" +
                "      '[disk/free]':\n" +
                "        enabled: ${inspectit.metrics.disk.enabled.free}\n" +
                "        type: LONG\n" +
                "        unit: bytes\n" +
                "        description: free disk space";

            InspectitConfig result = configParser.parseConfig(configYaml);

            // Build the expected result by hand
            Map<String, Boolean> enabledMap = new HashMap<>();
            enabledMap.put("free", true);
            StandardPollingMetricsRecorderSettings pollingSettings = new StandardPollingMetricsRecorderSettings();
            pollingSettings.setEnabled(enabledMap);

            MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                    .enabled(true).type(MetricDefinitionSettings.MeasureType.LONG).unit("bytes")
                    .description("free disk space").build();
            // I am not sure why adding views(null) to the builder is not allowed, but it isn't, so it's done afterwards.
            metricDefinition.setViews(null);
            Map<String, MetricDefinitionSettings> metricDefinitions = new HashMap<>();
            metricDefinitions.put("[disk/free]", metricDefinition);

            MetricsSettings metricsSettings = new MetricsSettings();
            metricsSettings.setDisk(pollingSettings);
            metricsSettings.setDefinitions(metricDefinitions);

            InspectitConfig expected = new InspectitConfig();
            expected.setMetrics(metricsSettings);

            assertEquals(expected, result);
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

            // Build the expected result by hand
            InternalSettings internal = new InternalSettings();
            internal.setInterBatchDelay(Duration.ofMillis(50));
            internal.setNewClassDiscoveryInterval(Duration.ofSeconds(10));

            InstrumentationSettings instrumentation = new InstrumentationSettings();
            instrumentation.setInternal(internal);

            InspectitConfig expected = new InspectitConfig();
            expected.setInstrumentation(instrumentation);

            assertEquals(expected, result);
        }

        @Test
        void withDocumentation() {

            final String configYaml =
                "inspectit:\n" +
                "  instrumentation:\n" +
                "    actions:\n" +
                "      a_debug_println:\n" +
                "        doc:\n" +
                "          description: 'Prints a given Object to stdout.'\n" +
                "          input-desc:\n" +
                "            value: Object to be printed\n" +
                "          return-desc: Void\n" +
                "        input:\n" +
                "          value: Object\n" +
                "        is-void: true\n" +
                "        value-body: |\n" +
                "          System.out.println(value);";

            InspectitConfig result = configParser.parseConfig(configYaml);

            // Build the expected result by hand
            Map<String, String> inputDescs = new HashMap<>();
            inputDescs.put("value", "Object to be printed");

            ActionDocSettings actionDoc = new ActionDocSettings();
            actionDoc.setDescription("Prints a given Object to stdout.");
            actionDoc.setInputDesc(inputDescs);
            actionDoc.setReturnDesc("Void");


            Map<String, String> inputs = new HashMap<>();
            inputs.put("value", "Object");
            GenericActionSettings action = new GenericActionSettings();
            action.setDoc(actionDoc);
            action.setInput(inputs);
            action.setIsVoid(true);
            action.setValueBody("System.out.println(value);");

            Map<String, GenericActionSettings> actions = new HashMap<>();
            actions.put("a_debug_println", action);

            InstrumentationSettings instrumentation = new InstrumentationSettings();
            instrumentation.setActions(actions);

            InspectitConfig expected = new InspectitConfig();
            expected.setInstrumentation(instrumentation);

            assertEquals(expected, result);
        }
    }
}