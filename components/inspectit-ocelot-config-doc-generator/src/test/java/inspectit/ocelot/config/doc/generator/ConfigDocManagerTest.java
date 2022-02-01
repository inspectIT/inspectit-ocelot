package inspectit.ocelot.config.doc.generator;

import com.google.common.io.Resources;
import inspectit.ocelot.config.doc.generator.docobjects.*;
import jdk.nashorn.internal.runtime.Scope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDocManagerTest {

    static ActionDoc actionWithDocInYaml;
    static ActionDoc actionWithoutDocInYaml;
    static ScopeDoc scopeDoc;
    static RuleDoc ruleDocChild;
    static RuleDoc ruleDocParent;
    static MetricDoc metricDoc;

    @BeforeAll
    static void createDocObjects(){
        List<ActionInputDoc> inputs1 = new ArrayList<>();
        inputs1.add(new ActionInputDoc("value", "Object", "Object to be printed"));
        actionWithDocInYaml = new ActionDoc("a_debug_println", "Prints a given Object to stdout.",
                inputs1, "void", true);

        List<ActionInputDoc> inputs2 = new ArrayList<>();
        inputs2.add(new ActionInputDoc("a", "Object", ""));
        inputs2.add(new ActionInputDoc("b", "Object", ""));
        actionWithoutDocInYaml = new ActionDoc("a_debug_println_2", "",
                inputs2, "", false);

        scopeDoc = new ScopeDoc("s_jdbc_statement_execute", "Scope for executed JDBC statements.");

        metricDoc = new MetricDoc("[disk/free]", "free disk space", "bytes");

        Map<String, Map<String, RuleActionCallDoc>> entryExits = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls = new HashMap<>();
        RuleActionCallDoc actionCallParent = new RuleActionCallDoc("method_name", "a_get_name");
        actionCalls.put("method_name", actionCallParent);
        entryExits.put("exit", actionCalls);
        ruleDocParent = new RuleDoc(
                "r_tracing_global_attributes", "", Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), null, entryExits);
        
        List<String> include = new ArrayList<>();
        include.add("r_tracing_global_attributes");
        include.add("r_capture_method_entry_timestamp_conditional");
        
        List<String> scopes = new ArrayList<>();
        scopes.add("s_httpurlconnection_connect");
        scopes.add("s_httpurlconnection_getOutputStream");
        
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
        
        Map<String, String> startSpanConditions = new HashMap<>();
        startSpanConditions.put("only-if-true", "jdbc_is_entry");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("db.type", "db_type_sql");
        attributes.put("db.url", "jdbc_url");
        RuleTracingDoc tracingDoc = new RuleTracingDoc(true, startSpanConditions, attributes);
        
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

    private String getYaml(String fileName){
        try {
            URL url = Resources.getResource("ConfigDocManagerTests/" + fileName);
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (Exception e){
            System.out.printf("Could not read YAML from file: %s%n", fileName);
            return null;
        }
    }

    ConfigDocManager configDocManager = new ConfigDocManager();

    @Test
    void loadActionDocWithDocInYaml() throws IOException {

        String configYaml = getYaml("actionWithDocInYaml.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<ActionDoc> actions = new ArrayList<>();
        actions.add(actionWithDocInYaml);
        ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                Collections.emptyList(), Collections.emptyList());

        assertEquals(expected, result);
    }

    @Test
    void loadActionDocWithoutDocInYaml(){
        String configYaml = getYaml("actionWithoutDocInYaml.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<ActionDoc> actions = new ArrayList<>();
        actions.add(actionWithoutDocInYaml);
        ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expected, result);
    }

    @Test
    void loadMultipleActions(){
        String configYaml = getYaml("multipleActions.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<ActionDoc> actions = new ArrayList<>();
        actions.add(actionWithDocInYaml);
        actions.add(actionWithoutDocInYaml);

        ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), actions,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expected, result);
    }

    @Test
    void loadScopeDoc(){
        String configYaml = getYaml("scope.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<ScopeDoc> scopes = new ArrayList<>();
        scopes.add(scopeDoc);

        ConfigDocumentation expected = new ConfigDocumentation(scopes, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expected, result);
    }

    @Test
    void loadMetricDoc(){
        String configYaml = getYaml("metric.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<MetricDoc> metrics = new ArrayList<>();
        metrics.add(metricDoc);

        ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), metrics);
        assertEquals(expected, result);
    }

    @Test
    void loadRuleDocs(){
        String configYaml = getYaml("rules.yml");
        ConfigDocumentation result = configDocManager.loadConfigDocumentation(configYaml);

        List<RuleDoc> rules = new ArrayList<>();
        rules.add(ruleDocChild);
        rules.add(ruleDocParent);

        ConfigDocumentation expected = new ConfigDocumentation(Collections.emptyList(), Collections.emptyList(),
                rules, Collections.emptyList());
        assertEquals(expected, result);
    }

    @Test
    void loadAll(){
        String configYaml = getYaml("all.yml");
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
        assertEquals(expected, result);
    }
}