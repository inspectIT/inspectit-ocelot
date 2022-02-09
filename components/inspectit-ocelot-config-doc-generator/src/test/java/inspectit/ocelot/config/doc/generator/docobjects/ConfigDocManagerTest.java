package inspectit.ocelot.config.doc.generator.docobjects;

import inspectit.ocelot.config.doc.generator.ConfigDocManager;
import inspectit.ocelot.config.doc.generator.docobjects.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDocManagerTest {

    static ActionDoc actionWithDocInYaml;
    static ActionDoc actionWithoutDocInYaml;
    static ScopeDoc scopeDoc;
    static RuleDoc ruleDocChild;
    static RuleDoc ruleDocParent;
    static MetricDoc metricDoc;

    /**
     * Instantiates the Doc-Objects that are later used by different Tests to test whether the ConfigDocumentation is
     * correctly generated.
     */
    @BeforeAll
    static void createDocObjects(){
        
        // Create a sample ActionDoc object where there was documentation values in the YAML, i.e. content behind the
        // doc-key
        List<ActionInputDoc> inputs1 = new ArrayList<>();
        inputs1.add(new ActionInputDoc("value", "Object", "Object to be printed"));
        actionWithDocInYaml = new ActionDoc("a_debug_println", "Prints a given Object to stdout.",
                inputs1, "Void", true);
        
        // Create a sample ActionDoc object where there was no documentation values in the YAML
        List<ActionInputDoc> inputs2 = new ArrayList<>();
        inputs2.add(new ActionInputDoc("a", "Object", ""));
        inputs2.add(new ActionInputDoc("b", "Object", ""));
        actionWithoutDocInYaml = new ActionDoc("a_debug_println_2", "",
                inputs2, "", false);

        // Create a sample ScopeDoc object
        scopeDoc = new ScopeDoc("s_jdbc_statement_execute", "Scope for executed JDBC statements.");

        // Create a sample MetricDoc object
        metricDoc = new MetricDoc("[disk/free]", "free disk space", "bytes");

        // Create a sample RuleDoc object to include inside of the other RuleDoc object
        // Create the entryExits-Map for the RuleDoc object
        Map<String, Map<String, RuleActionCallDoc>> entryExits = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls = new HashMap<>();
        RuleActionCallDoc actionCallParent = new RuleActionCallDoc("method_name", "a_get_name");
        actionCalls.put("method_name", actionCallParent);
        entryExits.put("exit", actionCalls);
        ruleDocParent = new RuleDoc(
                "r_tracing_global_attributes", "", Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), null, entryExits);
        
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
        metricsDocs.add(
                new RuleMetricsDoc("[service/in/responsetime]", "servicegraph_duration",
                        dataTags, constantTags));
        
        // Create the RuleTracingDoc for the RuleDoc
        Map<String, String> startSpanConditions = new HashMap<>();
        startSpanConditions.put("onlyIfTrue", "jdbc_is_entry");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("db.type", "db_type_sql");
        attributes.put("db.url", "jdbc_url");
        RuleTracingDoc tracingDoc = new RuleTracingDoc(true, startSpanConditions, attributes);
        
        // Create the entryExits Map for the RuleDoc
        Map<String, Map<String, RuleActionCallDoc>> entryExitsChild = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCallsChild = new HashMap<>();
        actionCallsChild.put("method_duration",
                new RuleActionCallDoc("method_duration", "a_timing_elapsedMillis"));
        actionCallsChild.put("method_name",
                new RuleActionCallDoc(actionCallParent, "r_tracing_global_attributes"));
        entryExitsChild.put("exit", actionCallsChild);

        ruleDocChild = new RuleDoc(
                "r_capture_method_duration_conditional", 
                "Conditionally captures the execution time of the current method into method_duration\n" + 
                        "The capturing will only happen it capture_time_condition is defined as true.\n" + 
                        "For example, http instrumentation define capture_time_condition based on http_is_entry\n" + 
                        "The condition is there to prevent unnecessary invocations of System.nanoTime(), which can be expensive", 
                include, scopes, metricsDocs, tracingDoc, entryExitsChild);
    }

    ConfigDocManager configDocManager = new ConfigDocManager();

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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<ActionDoc> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);
            ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                    Collections.emptyList(), Collections.emptyList());

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<ActionDoc> actions = new ArrayList<>();
            actions.add(actionWithoutDocInYaml);
            ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                    Collections.emptyList(), Collections.emptyList());

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<ActionDoc> actions = new ArrayList<>();
            actions.add(actionWithDocInYaml);
            actions.add(actionWithoutDocInYaml);

            ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                    Collections.emptyList(), Collections.emptyList());

            assertEquals(expected, result);
            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<ScopeDoc> scopes = new ArrayList<>();
            scopes.add(scopeDoc);

            ConfigDocumentation expected = new ConfigDocumentation(scopes, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<MetricDoc> metrics = new ArrayList<>();
            metrics.add(metricDoc);

            ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), metrics);

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

            List<RuleDoc> rules = new ArrayList<>();
            rules.add(ruleDocChild);
            rules.add(ruleDocParent);

            ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), Collections.emptyList(),
                    rules, Collections.emptyList());

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
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

            ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

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

            ConfigDocumentation expected = new ConfigDocumentation(scopes, actions,
                    rules, metrics);

            assertEquals(expected.getScopes(), result.getScopes());
            assertEquals(expected.getActions(), result.getActions());
            assertEquals(expected.getRules(), result.getRules());
            assertEquals(expected.getMetrics(), result.getMetrics());
        }
    }

}