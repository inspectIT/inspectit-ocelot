package inspectit.ocelot.config.doc.generator.docobjects;

import inspectit.ocelot.config.doc.generator.parsing.ConfigParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DocObjectGeneratorTest {

    private static ActionDoc actionWithDocInYaml;
    private static ActionDoc actionWithoutDocInYaml;
    private static ScopeDoc scopeDoc;
    private static RuleDoc ruleDocChild;
    private static RuleDoc ruleDocParent;
    private static MetricDoc metricDoc;

    /**
     * Instantiates the Doc-Objects that are later used by different Tests to test whether the ConfigDocumentation is
     * correctly scopeDoc.
     */
    @BeforeAll
    private static void createDocObjects(){

        // Create a sample ActionDoc object where there was documentation values in the YAML, i.e. content behind the
        // doc-key
        List<ActionInputDoc> inputs1 = new ArrayList<>();

        ActionInputDoc actionInputDocMock = Mockito.mock(ActionInputDoc.class);
        when(actionInputDocMock.getName()).thenReturn("value");
        when(actionInputDocMock.getType()).thenReturn("Object");
        when(actionInputDocMock.getDescription()).thenReturn("Object to be printed");
        inputs1.add(actionInputDocMock);

        actionWithDocInYaml = Mockito.mock(ActionDoc.class);
        when(actionWithDocInYaml.getName()).thenReturn("a_debug_println");
        when(actionWithDocInYaml.getDescription()).thenReturn("Prints a given Object to stdout.");
        when(actionWithDocInYaml.getInputs()).thenReturn(inputs1);
        when(actionWithDocInYaml.getReturnDescription()).thenReturn("Void");
        when(actionWithDocInYaml.getIsVoid()).thenReturn(true);

        // Create a sample ActionDoc object where there was no documentation values in the YAML
        List<ActionInputDoc> inputs2 = new ArrayList<>();

        ActionInputDoc actionInputDocMock2 = Mockito.mock(ActionInputDoc.class);
        when(actionInputDocMock2.getName()).thenReturn("a");
        when(actionInputDocMock2.getType()).thenReturn("Object");
        when(actionInputDocMock2.getDescription()).thenReturn("");
        inputs2.add(actionInputDocMock2);

        ActionInputDoc actionInputDocMock3 = Mockito.mock(ActionInputDoc.class);;
        when(actionInputDocMock3.getName()).thenReturn("b");
        when(actionInputDocMock3.getType()).thenReturn("Object");
        when(actionInputDocMock3.getDescription()).thenReturn("");
        inputs2.add(actionInputDocMock3);

        actionWithoutDocInYaml = Mockito.mock(ActionDoc.class);
        when(actionWithoutDocInYaml.getName()).thenReturn("a_debug_println_2");
        when(actionWithoutDocInYaml.getDescription()).thenReturn("");
        when(actionWithoutDocInYaml.getInputs()).thenReturn(inputs2);
        when(actionWithoutDocInYaml.getReturnDescription()).thenReturn("");
        when(actionWithoutDocInYaml.getIsVoid()).thenReturn(false);

        // Create a sample ScopeDoc object
        scopeDoc = Mockito.mock(ScopeDoc.class);
        when(scopeDoc.getName()).thenReturn("s_jdbc_statement_execute");
        when(scopeDoc.getDescription()).thenReturn("Scope for executed JDBC statements.");

        // Create a sample MetricDoc object
        metricDoc = Mockito.mock(MetricDoc.class);
        when(metricDoc.getName()).thenReturn("[disk/free]");
        when(metricDoc.getDescription()).thenReturn("free disk space");
        when(metricDoc.getUnit()).thenReturn("bytes");

        // Create a sample RuleDoc object to include inside of the other RuleDoc object
        // Create the entryExits-Map for the RuleDoc object
        Map<String, Map<String, RuleActionCallDoc>> entryExitsParent = RuleDocTest.getEmptyEntryExits();
        Map<String, RuleActionCallDoc> actionCallsParent = new HashMap<>();

        RuleActionCallDoc actionCallParentMock = Mockito.mock(RuleActionCallDoc.class);
        final String actionCallParentName = "method_name";
        final String actionCallParentAction = "a_get_name";
        when(actionCallParentMock.getName()).thenReturn(actionCallParentName);
        when(actionCallParentMock.getAction()).thenReturn(actionCallParentAction);

        actionCallsParent.put("method_name", actionCallParentMock);
        entryExitsParent.put("exit", actionCallsParent);

        ruleDocParent = Mockito.mock(RuleDoc.class);
        when(ruleDocParent.getName()).thenReturn("r_tracing_global_attributes");
        when(ruleDocParent.getDescription()).thenReturn("");
        when(ruleDocParent.getEntryExits()).thenReturn(entryExitsParent);

        // Create a sample RuleDoc with examples for all possible contents
        // Create the includes list for the RuleDoc
        List<String> include = new ArrayList<>();
        include.add("r_tracing_global_attributes");
        include.add("r_capture_method_entry_timestamp_conditional");

        // Create the scopes list for the RuleDoc
        List<String> scopes = new ArrayList<>();
        scopes.add("s_httpurlconnection_connect");
        scopes.add("s_httpurlconnection_getOutputStream");

        // Create the RuleMetricsDoc list for the RuleDoc
        List<RuleMetricsDoc> metricsDocs = new ArrayList<>();
        Map<String, String> dataTags = new HashMap<>();
        dataTags.put("origin_service", "servicegraph_origin_service_local");
        dataTags.put("origin_external", "servicegraph_origin_external");
        Map<String, String> constantTags = new HashMap<>();
        constantTags.put("protocol", "servicegraph_protocol");
        constantTags.put("error", "servicegraph_is_error");

        RuleMetricsDoc ruleMetricsDocMock = Mockito.mock(RuleMetricsDoc.class);
        when(ruleMetricsDocMock.getName()).thenReturn("[service/in/responsetime]");
        when(ruleMetricsDocMock.getValue()).thenReturn("servicegraph_duration");
        when(ruleMetricsDocMock.getDataTags()).thenReturn(dataTags);
        when(ruleMetricsDocMock.getConstantTags()).thenReturn(constantTags);
        metricsDocs.add(ruleMetricsDocMock);

        // Create the RuleTracingDoc for the RuleDoc
        Map<String, String> startSpanConditions = new HashMap<>();
        startSpanConditions.put("onlyIfTrue", "jdbc_is_entry");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("db.type", "db_type_sql");
        attributes.put("db.url", "jdbc_url");

        RuleTracingDoc tracingDocMock = Mockito.mock(RuleTracingDoc.class);
        when(tracingDocMock.getStartSpan()).thenReturn(true);
        when(tracingDocMock.getStartSpanConditions()).thenReturn(startSpanConditions);
        when(tracingDocMock.getAttributes()).thenReturn(attributes);

        // Create the entryExits Map for the RuleDoc
        Map<String, Map<String, RuleActionCallDoc>> entryExitsChild = RuleDocTest.getEmptyEntryExits();
        Map<String, RuleActionCallDoc> actionCallsChild = new HashMap<>();

        RuleActionCallDoc actionCallChildMock1 = Mockito.mock(RuleActionCallDoc.class);
        when(actionCallChildMock1.getName()).thenReturn("method_duration");
        when(actionCallChildMock1.getAction()).thenReturn("a_timing_elapsedMillis");
        actionCallsChild.put("method_duration", actionCallChildMock1);

        RuleActionCallDoc actionCallChildMock2 = Mockito.mock(RuleActionCallDoc.class);
        when(actionCallChildMock2.getName()).thenReturn(actionCallParentName);
        when(actionCallChildMock2.getAction()).thenReturn(actionCallParentAction);
        when(actionCallChildMock2.getInheritedFrom()).thenReturn("r_tracing_global_attributes");
        actionCallsChild.put("method_name", actionCallChildMock2);

        entryExitsChild.put("exit", actionCallsChild);

        ruleDocChild = Mockito.mock(RuleDoc.class);
        when(ruleDocChild.getName()).thenReturn("r_capture_method_duration_conditional");
        when(ruleDocChild.getDescription()).thenReturn(
                "Conditionally captures the execution time of the current method into method_duration\n" +
                "The capturing will only happen it capture_time_condition is defined as true.\n" +
                "For example, http instrumentation define capture_time_condition based on http_is_entry\n" +
                "The condition is there to prevent unnecessary invocations of System.nanoTime(), which can be expensive");
        when(ruleDocChild.getInclude()).thenReturn(include);
        when(ruleDocChild.getScopes()).thenReturn(scopes);
        when(ruleDocChild.getMetricsDocs()).thenReturn(metricsDocs);
        when(ruleDocChild.getTracingDoc()).thenReturn(tracingDocMock);
        when(ruleDocChild.getEntryExits()).thenReturn(entryExitsChild);
    }

    private final DocObjectGenerator docObjectGenerator = new DocObjectGenerator();

    // Needed to create InspectitConfig Objects to use as input for DocObjectGenerator in tests.
    private final ConfigParser configParser = new ConfigParser();

    @Nested
    class LoadConfigDocumentationTests{

        @Test
        void loadActionDocWithDocInYaml() throws IOException {

            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    actions:\n" +
                    "      # Prints a given Object to stdout\n" +
                    "      a_debug_println:\n" +
                    "        doc:\n" +
                    "          description: 'Prints a given Object to stdout.'\n" +
                    "          input-desc:\n" +
                    "            value: Object to be printed\n" +
                    "          return-desc: Void\n" +
                    "        input:\n" +
                    "          value: Object\n" +
                    "        is-void: true\n" +
                    "        value: System.out.println(value);";

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<ActionDoc> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadActionDocWithoutDocInYaml(){
            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    actions:\n" +
                    "      # Prints two given Objects to stdout\n" +
                    "      a_debug_println_2:\n" +
                    "        imports:\n" +
                    "          - java.util\n" +
                    "        input:\n" +
                    "          a: Object\n" +
                    "          b: Object\n" +
                    "        value-body: |\n" +
                    "          System.out.println(a + \\\"\\\" + b);\n" +
                    "          return a + \\\"\\\" b;\";";

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<ActionDoc> actions = new ArrayList<>();
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(actions);
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadMultipleActions(){
            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    actions:\n" +
                    "      # Prints two given Objects to stdout\n" +
                    "      a_debug_println_2:\n" +
                    "        imports:\n" +
                    "          - java.util\n" +
                    "        input:\n" +
                    "          a: Object\n" +
                    "          b: Object\n" +
                    "        value-body: |\n" +
                    "          System.out.println(a + \\\"\\\" + b);\n" +
                    "          return a + \\\"\\\" b;\";\n" +
                    "      # Prints a given Object to stdout\n" +
                    "      a_debug_println:\n" +
                    "        doc:\n" +
                    "          description: 'Prints a given Object to stdout.'\n" +
                    "          input-desc:\n" +
                    "            value: Object to be printed\n" +
                    "          return-desc: Void\n" +
                    "        input:\n" +
                    "          value: Object\n" +
                    "        is-void: true\n" +
                    "        value: System.out.println(value);";

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<ActionDoc> actions = new ArrayList<>();
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
        void loadScopeDoc(){
            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    scopes:\n" +
                    "      s_jdbc_statement_execute:\n" +
                    "        doc:\n" +
                    "          description: 'Scope for executed JDBC statements.'\n" +
                    "        superclass:\n" +
                    "          name: java.net.HttpURLConnection\n" +
                    "        interfaces:\n" +
                    "          - name: java.sql.Statement\n" +
                    "        methods:\n" +
                    "          - name: execute\n" +
                    "          - name: executeQuery\n" +
                    "          - name: executeUpdate\n" +
                    "        advanced:\n" +
                    "          instrument-only-inherited-methods: true";

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<ScopeDoc> scopes = new ArrayList<>();
            scopes.add(scopeDoc);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(scopes);
            when(expected.getActions()).thenReturn(Collections.emptyList());
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(Collections.emptyList());

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadMetricDoc(){
            String configYaml =
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

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<MetricDoc> metrics = new ArrayList<>();
            metrics.add(metricDoc);

            ConfigDocumentation expected = Mockito.mock(ConfigDocumentation.class);
            when(expected.getScopes()).thenReturn(Collections.emptyList());
            when(expected.getActions()).thenReturn(Collections.emptyList());
            when(expected.getRules()).thenReturn(Collections.emptyList());
            when(expected.getMetrics()).thenReturn(metrics);

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void loadRuleDocs(){
            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    rules:\n" +
                    "      r_tracing_global_attributes:\n" +
                    "        exit:\n" +
                    "          method_name:\n" +
                    "            action: a_get_name\n" +
                    "\n" +
                    "      r_capture_method_duration_conditional:\n" +
                    "        doc:\n" +
                    "          description: |-\n" +
                    "            Conditionally captures the execution time of the current method into method_duration\n" +
                    "            The capturing will only happen it capture_time_condition is defined as true.\n" +
                    "            For example, http instrumentation define capture_time_condition based on http_is_entry\n" +
                    "            The condition is there to prevent unnecessary invocations of System.nanoTime(), which can be expensive\n" +
                    "        include:\n" +
                    "          r_tracing_global_attributes: true\n" +
                    "          r_capture_method_entry_timestamp_conditional: true\n" +
                    "          r_capture_ddd: false\n" +
                    "        scopes:\n" +
                    "          s_httpurlconnection_connect: true\n" +
                    "          s_httpurlconnection_getOutputStream: true\n" +
                    "        exit:\n" +
                    "          method_duration:\n" +
                    "            only-if-true: capture_time_condition\n" +
                    "            action: a_timing_elapsedMillis\n" +
                    "            data-input:\n" +
                    "              since_nanos: method_entry_time\n" +
                    "            constant-input:\n" +
                    "              value: sql\n" +
                    "        tracing:\n" +
                    "          start-span: true\n" +
                    "          start-span-conditions:\n" +
                    "            only-if-true: jdbc_is_entry\n" +
                    "          attributes:\n" +
                    "            db.type: db_type_sql\n" +
                    "            db.url: jdbc_url\n" +
                    "          error-status: _thrown\n" +
                    "        metrics:\n" +
                    "          '[service/in/responsetime]':\n" +
                    "            value: servicegraph_duration\n" +
                    "            data-tags:\n" +
                    "              origin_service: servicegraph_origin_service_local\n" +
                    "              origin_external: servicegraph_origin_external\n" +
                    "            constant-tags:\n" +
                    "              protocol: servicegraph_protocol\n" +
                    "              error: servicegraph_is_error";

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<RuleDoc> rules = new ArrayList<>();
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
        void loadAll(){
            String configYaml =
                    "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    actions:\n" +
                    "      # Prints two given Objects to stdout\n" +
                    "      a_debug_println_2:\n" +
                    "        imports:\n" +
                    "          - java.util\n" +
                    "        input:\n" +
                    "          a: Object\n" +
                    "          b: Object\n" +
                    "        value-body: |\n" +
                    "          System.out.println(a + \\\"\\\" + b);\n" +
                    "          return a + \\\"\\\" b;\";\n" +
                    "      # Prints a given Object to stdout\n" +
                    "      a_debug_println:\n" +
                    "        doc:\n" +
                    "          description: 'Prints a given Object to stdout.'\n" +
                    "          input-desc:\n" +
                    "            value: Object to be printed\n" +
                    "          return-desc: Void\n" +
                    "        input:\n" +
                    "          value: Object\n" +
                    "        is-void: true\n" +
                    "        value: System.out.println(value);\n" +
                    "\n" +
                    "    scopes:\n" +
                    "      s_jdbc_statement_execute:\n" +
                    "        doc:\n" +
                    "          description: 'Scope for executed JDBC statements.'\n" +
                    "        superclass:\n" +
                    "          name: java.net.HttpURLConnection\n" +
                    "        interfaces:\n" +
                    "          - name: java.sql.Statement\n" +
                    "        methods:\n" +
                    "          - name: execute\n" +
                    "          - name: executeQuery\n" +
                    "          - name: executeUpdate\n" +
                    "        advanced:\n" +
                    "          instrument-only-inherited-methods: true\n" +
                    "      \n" +
                    "    rules:\n" +
                    "      r_tracing_global_attributes:\n" +
                    "        exit:\n" +
                    "          method_name:\n" +
                    "            action: a_get_name\n" +
                    "\n" +
                    "      r_capture_method_duration_conditional:\n" +
                    "        doc:\n" +
                    "          description: |-\n" +
                    "            Conditionally captures the execution time of the current method into method_duration\n" +
                    "            The capturing will only happen it capture_time_condition is defined as true.\n" +
                    "            For example, http instrumentation define capture_time_condition based on http_is_entry\n" +
                    "            The condition is there to prevent unnecessary invocations of System.nanoTime(), which can be expensive\n" +
                    "        include:\n" +
                    "          r_tracing_global_attributes: true\n" +
                    "          r_capture_method_entry_timestamp_conditional: true\n" +
                    "          r_capture_ddd: false\n" +
                    "        scopes:\n" +
                    "          s_httpurlconnection_connect: true\n" +
                    "          s_httpurlconnection_getOutputStream: true\n" +
                    "        exit:\n" +
                    "          method_duration:\n" +
                    "            only-if-true: capture_time_condition\n" +
                    "            action: a_timing_elapsedMillis\n" +
                    "            data-input:\n" +
                    "              since_nanos: method_entry_time\n" +
                    "            constant-input:\n" +
                    "              value: sql\n" +
                    "        tracing:\n" +
                    "          start-span: true\n" +
                    "          start-span-conditions:\n" +
                    "            only-if-true: jdbc_is_entry\n" +
                    "          attributes:\n" +
                    "            db.type: db_type_sql\n" +
                    "            db.url: jdbc_url\n" +
                    "          error-status: _thrown\n" +
                    "        metrics:\n" +
                    "          '[service/in/responsetime]':\n" +
                    "            value: servicegraph_duration\n" +
                    "            data-tags:\n" +
                    "              origin_service: servicegraph_origin_service_local\n" +
                    "              origin_external: servicegraph_origin_external\n" +
                    "            constant-tags:\n" +
                    "              protocol: servicegraph_protocol\n" +
                    "              error: servicegraph_is_error\n" +
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

            InspectitConfig config = configParser.parseConfig(configYaml);
            ConfigDocumentation result = docObjectGenerator.generateConfigDocumentation(config);

            List<RuleDoc> rules = new ArrayList<>();
            rules.add(ruleDocChild);
            rules.add(ruleDocParent);
            List<MetricDoc> metrics = new ArrayList<>();
            metrics.add(metricDoc);
            List<ScopeDoc> scopes = new ArrayList<>();
            scopes.add(scopeDoc);
            List<ActionDoc> actions = new ArrayList<>();
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