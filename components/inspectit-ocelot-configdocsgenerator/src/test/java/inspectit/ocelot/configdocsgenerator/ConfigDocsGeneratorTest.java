package inspectit.ocelot.configdocsgenerator;

import com.google.common.io.Resources;
import inspectit.ocelot.configdocsgenerator.model.*;
import inspectit.ocelot.configdocsgenerator.parsing.ConfigParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ConfigDocsGeneratorTest {

    private static ActionDocs actionWithDocInYaml;

    private static ActionDocs actionWithoutDocInYaml;

    private static BaseDocs scopeDoc;

    private static RuleDocs ruleDocChild;

    private static RuleDocs ruleDocParent;

    private static MetricDocs metricDoc;

    private final static ConfigDocsGenerator configDocsGenerator = new ConfigDocsGenerator();

    /**
     * Needed to create InspectitConfig Objects to use as input for DocObjectGenerator in tests.
     */
    private final ConfigParser configParser = new ConfigParser();

    /**
     * Helper method to read Yaml from resources.
     *
     * @param fileName Name of yml File in resources.
     *
     * @return String containing the Yaml from the yml file.
     */
    private String getYaml(String fileName) throws IOException {
        URL url = Resources.getResource("ConfigDocGeneratorTest/" + fileName);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }

    /**
     * Instantiates the Doc-Objects that are later used by different Tests to test whether the ConfigDocumentation is
     * correctly scopeDoc.
     */
    @BeforeAll
    private static void createDocObjects() {

        // Create a sample ActionDoc object where there was documentation values in the YAML, i.e. content behind the
        // doc-key
        List<ActionInputDocs> inputs1 = new ArrayList<>();

        ActionInputDocs actionInputDocsMock1 = Mockito.mock(ActionInputDocs.class);
        when(actionInputDocsMock1.getName()).thenReturn("_arg1");
        when(actionInputDocsMock1.getType()).thenReturn("Object");
        when(actionInputDocsMock1.getDescription()).thenReturn("The _argN-th argument with which the instrumented method was called within which this action is getting executed.");
        inputs1.add(actionInputDocsMock1);
        ActionInputDocs actionInputDocsMock2 = Mockito.mock(ActionInputDocs.class);
        when(actionInputDocsMock2.getName()).thenReturn("_attachments");
        when(actionInputDocsMock2.getType()).thenReturn("ObjectAttachments");
        when(actionInputDocsMock2.getDescription()).thenReturn("Allows you to attach values to objects instead of to the control flow, as done via _context.");
        inputs1.add(actionInputDocsMock2);
        ActionInputDocs actionInputDocsMock3 = Mockito.mock(ActionInputDocs.class);
        when(actionInputDocsMock3.getName()).thenReturn("value");
        when(actionInputDocsMock3.getType()).thenReturn("Object");
        when(actionInputDocsMock3.getDescription()).thenReturn("Object to be printed");
        inputs1.add(actionInputDocsMock3);

        actionWithDocInYaml = Mockito.mock(ActionDocs.class);
        when(actionWithDocInYaml.getName()).thenReturn("a_debug_println");
        when(actionWithDocInYaml.getDescription()).thenReturn("Prints a given Object to stdout.");
        when(actionWithDocInYaml.getSince()).thenReturn("1.0");
        when(actionWithDocInYaml.getInputs()).thenReturn(inputs1);
        when(actionWithDocInYaml.getReturnDescription()).thenReturn("Void");
        when(actionWithDocInYaml.getIsVoid()).thenReturn(true);

        // Create a sample ActionDoc object where there was no documentation values in the YAML
        List<ActionInputDocs> inputs2 = new ArrayList<>();

        ActionInputDocs actionInputDocsMock4 = Mockito.mock(ActionInputDocs.class);
        when(actionInputDocsMock4.getName()).thenReturn("a");
        when(actionInputDocsMock4.getType()).thenReturn("Object");
        when(actionInputDocsMock4.getDescription()).thenReturn("");
        inputs2.add(actionInputDocsMock4);

        ActionInputDocs actionInputDocsMock5 = Mockito.mock(ActionInputDocs.class);
        when(actionInputDocsMock5.getName()).thenReturn("b");
        when(actionInputDocsMock5.getType()).thenReturn("Object");
        when(actionInputDocsMock5.getDescription()).thenReturn("");
        inputs2.add(actionInputDocsMock5);

        actionWithoutDocInYaml = Mockito.mock(ActionDocs.class);
        when(actionWithoutDocInYaml.getName()).thenReturn("a_debug_println_2");
        when(actionWithoutDocInYaml.getDescription()).thenReturn("");
        when(actionWithoutDocInYaml.getSince()).thenReturn("");
        when(actionWithoutDocInYaml.getInputs()).thenReturn(inputs2);
        when(actionWithoutDocInYaml.getReturnDescription()).thenReturn("");
        when(actionWithoutDocInYaml.getIsVoid()).thenReturn(false);

        // Create a sample ScopeDoc object
        scopeDoc = Mockito.mock(BaseDocs.class);
        when(scopeDoc.getName()).thenReturn("s_jdbc_statement_execute");
        when(scopeDoc.getDescription()).thenReturn("Scope for executed JDBC statements.");
        when(scopeDoc.getSince()).thenReturn("");

        // Create a sample MetricDoc object
        metricDoc = Mockito.mock(MetricDocs.class);
        when(metricDoc.getName()).thenReturn("disk/free");
        when(metricDoc.getDescription()).thenReturn("free disk space");
        when(metricDoc.getSince()).thenReturn(null);
        when(metricDoc.getUnit()).thenReturn("bytes");

        // Create a sample RuleDoc object to include inside of the other RuleDoc object
        // Create the actionCallsMap for the RuleDoc object
        Map<String, Map<String, ActionCallDocs>> actionCallsMapParent = RuleDocsTest.getEmptyActionCallsMap();
        Map<String, ActionCallDocs> actionCallsParent = new HashMap<>();

        ActionCallDocs actionCallParentMock = Mockito.mock(ActionCallDocs.class);
        final String actionCallParentName = "method_name";
        final String actionCallParentAction = "a_get_name";
        when(actionCallParentMock.getName()).thenReturn(actionCallParentName);
        when(actionCallParentMock.getActionName()).thenReturn(actionCallParentAction);

        actionCallsParent.put("method_name", actionCallParentMock);
        actionCallsMapParent.put("exit", actionCallsParent);

        ruleDocParent = Mockito.mock(RuleDocs.class);
        when(ruleDocParent.getName()).thenReturn("r_tracing_global_attributes");
        when(ruleDocParent.getDescription()).thenReturn("");
        when(ruleDocParent.getSince()).thenReturn("");
        when(ruleDocParent.getActionCallsMap()).thenReturn(actionCallsMapParent);

        // Create a sample RuleDoc with examples for all possible contents
        // Create the includes list for the RuleDoc
        List<String> include = new ArrayList<>();
        include.add("r_capture_method_entry_timestamp_conditional");
        include.add("r_tracing_global_attributes");

        // Create the scopes list for the RuleDoc
        List<String> scopes = new ArrayList<>();
        scopes.add("s_httpurlconnection_connect");
        scopes.add("s_httpurlconnection_getOutputStream");

        // Create the RuleMetricsDoc list for the RuleDoc
        List<RuleMetricsDocs> metricsDocs = new ArrayList<>();
        Map<String, String> dataTags = new HashMap<>();
        dataTags.put("origin_service", "servicegraph_origin_service_local");
        dataTags.put("origin_external", "servicegraph_origin_external");
        Map<String, String> constantTags = new HashMap<>();
        constantTags.put("protocol", "servicegraph_protocol");
        constantTags.put("error", "servicegraph_is_error");

        RuleMetricsDocs ruleMetricsDocsMock = Mockito.mock(RuleMetricsDocs.class);
        when(ruleMetricsDocsMock.getName()).thenReturn("service/in/responsetime");
        when(ruleMetricsDocsMock.getValue()).thenReturn("servicegraph_duration");
        when(ruleMetricsDocsMock.getDataTags()).thenReturn(dataTags);
        when(ruleMetricsDocsMock.getConstantTags()).thenReturn(constantTags);
        metricsDocs.add(ruleMetricsDocsMock);

        // Create the RuleTracingDoc for the RuleDoc
        Map<String, String> startSpanConditions = new HashMap<>();
        startSpanConditions.put("onlyIfTrue", "jdbc_is_entry");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("db.type", "db_type_sql");
        attributes.put("db.url", "jdbc_url");

        RuleTracingDocs tracingDocMock = Mockito.mock(RuleTracingDocs.class);
        when(tracingDocMock.getStartSpan()).thenReturn(true);
        when(tracingDocMock.getStartSpanConditions()).thenReturn(startSpanConditions);
        when(tracingDocMock.getAttributes()).thenReturn(attributes);

        // Create the actionCallsMap for the RuleDoc
        Map<String, Map<String, ActionCallDocs>> actionCallsMapChild = RuleDocsTest.getEmptyActionCallsMap();
        Map<String, ActionCallDocs> actionCallsChild = new HashMap<>();

        ActionCallDocs actionCallChildMock1 = Mockito.mock(ActionCallDocs.class);
        when(actionCallChildMock1.getName()).thenReturn("method_duration");
        when(actionCallChildMock1.getActionName()).thenReturn("a_timing_elapsedMillis");
        actionCallsChild.put("method_duration", actionCallChildMock1);

        ActionCallDocs actionCallChildMock2 = Mockito.mock(ActionCallDocs.class);
        when(actionCallChildMock2.getName()).thenReturn(actionCallParentName);
        when(actionCallChildMock2.getActionName()).thenReturn(actionCallParentAction);
        when(actionCallChildMock2.getInheritedFrom()).thenReturn("r_tracing_global_attributes");
        actionCallsChild.put("method_name", actionCallChildMock2);

        actionCallsMapChild.put("exit", actionCallsChild);

        ruleDocChild = Mockito.mock(RuleDocs.class);
        when(ruleDocChild.getName()).thenReturn("r_capture_method_duration_conditional");
        when(ruleDocChild.getDescription()).thenReturn("Conditionally captures\n" + "linebreak.");
        when(ruleDocChild.getSince()).thenReturn("");
        when(ruleDocChild.getInclude()).thenReturn(include);
        when(ruleDocChild.getScopes()).thenReturn(scopes);
        when(ruleDocChild.getMetricsDocs()).thenReturn(metricsDocs);
        when(ruleDocChild.getTracingDoc()).thenReturn(tracingDocMock);
        when(ruleDocChild.getActionCallsMap()).thenReturn(actionCallsMapChild);
    }

    @Nested
    class LoadConfigDocumentationTests {

        @Test
        void loadActionDocWithDocInYaml() throws IOException {
            String configYaml = getYaml("actionWithDocInYaml.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<ActionDocs> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadActionDocWithoutDocInYaml() throws IOException {
            String configYaml = getYaml("actionWithoutDocInYaml.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<ActionDocs> actions = new ArrayList<>();
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadMultipleActions() throws IOException {
            String configYaml = getYaml("multipleActions.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<ActionDocs> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadScopeDoc() throws IOException {
            String configYaml = getYaml("scope.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<BaseDocs> scopes = new ArrayList<>();
            scopes.add(scopeDoc);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(scopes);
            when(expected.getActions()).thenReturn(Collections.emptyList());
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadMetricDoc() throws IOException {
            String configYaml = getYaml("metric.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<MetricDocs> metrics = new ArrayList<>();
            metrics.add(metricDoc);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(Collections.emptyList());
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(metrics);

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadRuleDocs() throws IOException {
            String configYaml = getYaml("rules.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<RuleDocs> rules = new ArrayList<>();
            rules.add(ruleDocChild);
            rules.add(ruleDocParent);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(Collections.emptyList());
            when(expected.getRules()).thenReturn(rules);
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadAll() throws IOException {
            String configYaml = getYaml("all.yml");

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<RuleDocs> rules = new ArrayList<>();
            rules.add(ruleDocChild);
            rules.add(ruleDocParent);
            List<MetricDocs> metrics = new ArrayList<>();
            metrics.add(metricDoc);
            List<BaseDocs> scopes = new ArrayList<>();
            scopes.add(scopeDoc);
            List<ActionDocs> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(scopes);
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(rules);
            when(expected.getMetrics()).thenReturn(metrics);

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Nested
    class FindFiles {

        @Test
        void verifyFindFiles() throws IOException {
            String file = "all.yml";
            String configYaml = getYaml(file);

            Set<String> docsObjects = new HashSet<>();
            docsObjects.add(actionWithDocInYaml.getName());
            docsObjects.add(actionWithoutDocInYaml.getName());
            docsObjects.add(scopeDoc.getName());
            docsObjects.add(ruleDocChild.getName());
            docsObjects.add(ruleDocParent.getName());
            docsObjects.add(metricDoc.getName());

            Map<String, Set<String>> docsObjectsByFile = Collections.singletonMap(file, docsObjects);
            configDocsGenerator.setDocsObjectsByFile(docsObjectsByFile);
            when(actionWithDocInYaml.getFiles()).thenReturn(Collections.singleton(file));
            when(actionWithoutDocInYaml.getFiles()).thenReturn(Collections.singleton(file));
            when(scopeDoc.getFiles()).thenReturn(Collections.singleton(file));
            when(ruleDocChild.getFiles()).thenReturn(Collections.singleton(file));
            when(ruleDocParent.getFiles()).thenReturn(Collections.singleton(file));
            when(metricDoc.getFiles()).thenReturn(Collections.singleton(file));

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = configDocsGenerator.generateConfigDocs(config);

            List<RuleDocs> rules = new ArrayList<>();
            rules.add(ruleDocChild);
            rules.add(ruleDocParent);
            List<MetricDocs> metrics = new ArrayList<>();
            metrics.add(metricDoc);
            List<BaseDocs> scopes = new ArrayList<>();
            scopes.add(scopeDoc);
            List<ActionDocs> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(scopes);
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(rules);
            when(expected.getMetrics()).thenReturn(metrics);

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }
    }
}
