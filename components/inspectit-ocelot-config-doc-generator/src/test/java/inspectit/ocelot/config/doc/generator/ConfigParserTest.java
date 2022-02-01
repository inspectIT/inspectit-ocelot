package inspectit.ocelot.config.doc.generator;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
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

    private String getYaml(String fileName){
        try {
            URL url = Resources.getResource("ConfigParserTest/" + fileName);
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (Exception e){
            System.out.printf("Could not read YAML from file: %s%n", fileName);
            return null;
        }
    }

    private final ConfigParser configParser = new ConfigParser();

    @Test
    void replacePlaceholders() {

        String configYaml =
                "inspectit:\n" +
                // Test replacement of placeholders at different deepness levels
                "  placeholder-test: ${inspectit.name.placeholder-value}\n" +
                "  placeholder-test2: ${inspectit.name.second.placeholder-value}\n" +
                // Test replacement if placeholder-keys do not lead to a value
                "  placeholder-test3: ${inspectit.doesnotexist}\n" +
                "  name:\n" +
                "    placeholder-value: value\n" +
                "    second:\n" +
                "      placeholder-value: value2";

        String result = configParser.replacePlaceholders(configYaml);

        String expected =
                "inspectit:\n" +
                "  placeholder-test: value\n" +
                "  placeholder-test2: value2\n" +
                "  placeholder-test3: inspectit.doesnotexist\n" +
                "  name:\n" +
                "    placeholder-value: value\n" +
                "    second:\n" +
                "      placeholder-value: value2";

        assertEquals(expected, result);
    }

    @Test
    void parseConfigPlaceholder() {

        String configYaml = getYaml("placeholder.yml");
        InspectitConfig result = configParser.parseConfig(configYaml);

        Map<String, Boolean> enabledMap = new HashMap<>();
        enabledMap.put("free", true);
        StandardPollingMetricsRecorderSettings pollingSettings = new StandardPollingMetricsRecorderSettings();
        pollingSettings.setEnabled(enabledMap);

        MetricDefinitionSettings metricDefinition = MetricDefinitionSettings.builder()
                .enabled(true).type(MetricDefinitionSettings.MeasureType.LONG).unit("bytes")
                .description("free disk space").build();
        // I am not sure why adding views(null) to the builder is not allowed?
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
    void parseConfigDuration() {

        String configYaml = getYaml("duration.yml");
        InspectitConfig result = configParser.parseConfig(configYaml);

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
    void parseConfigDocumentation() {

        String configYaml = getYaml("documentation.yml");
        InspectitConfig result = configParser.parseConfig(configYaml);

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