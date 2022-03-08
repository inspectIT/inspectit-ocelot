package inspectit.ocelot.configdocsgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import inspectit.ocelot.configdocsgenerator.model.*;
import inspectit.ocelot.configdocsgenerator.parsing.ConfigParser;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.ActionDocumentation;
import rocks.inspectit.ocelot.config.model.instrumentation.documentation.BaseDocumentation;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generator for generating ConfigDocumentation objects based on a YAML String describing an {@link InspectitConfig} or the
 * InspectitConfig itself.
 */
public class ConfigDocsGenerator {

    /**
     * Generates a ConfigDocumentation from a YAML String describing an {@link InspectitConfig}.
     *
     * @param configYaml YAML String describing an {@link InspectitConfig}.
     *
     * @return Returns the generated ConfigDocumentation.
     */
    public ConfigDocumentation generateConfigDocs(String configYaml) throws JsonProcessingException {
        ConfigParser configParser = new ConfigParser();
        InspectitConfig config = configParser.parseConfig(configYaml);

        return generateConfigDocs(config);
    }

    /**
     * Generates a ConfigDocumentation from an {@link InspectitConfig} object.
     *
     * @param config The input {@link InspectitConfig}.
     *
     * @return Returns the generated ConfigDocumentation.
     */
    public ConfigDocumentation generateConfigDocs(InspectitConfig config) {

        InstrumentationSettings instrumentation = config.getInstrumentation();

        if (instrumentation == null) {
            instrumentation = new InstrumentationSettings();
        }
        Map<String, InstrumentationScopeSettings> scopes = instrumentation.getScopes();
        Map<String, GenericActionSettings> actions = instrumentation.getActions();
        Map<String, InstrumentationRuleSettings> rules = instrumentation.getRules();

        List<BaseDocs> scopesDocs = generateScopeDocs(scopes);
        scopesDocs.sort(Comparator.comparing(BaseDocs::getName));

        List<ActionDocs> actionsDocs = generateActionDocs(actions);
        actionsDocs.sort(Comparator.comparing(BaseDocs::getName));

        Map<String, RuleDocs> ruleDocsMap = generateRuleDocsMap(rules);
        for (RuleDocs currentRule : ruleDocsMap.values()) {
            currentRule.addActionCallsFromIncludedRules(ruleDocsMap, currentRule.getInclude());
        }
        List<RuleDocs> ruleDocs = new ArrayList<>(ruleDocsMap.values());
        ruleDocs.sort(Comparator.comparing(BaseDocs::getName));

        Map<String, MetricDefinitionSettings> metrics = config.getMetrics().getDefinitions();
        List<MetricDocs> metricDocs = generateMetricDocs(metrics);
        metricDocs.sort(Comparator.comparing(BaseDocs::getName));

        return new ConfigDocumentation(scopesDocs, actionsDocs, ruleDocs, metricDocs);
    }

    /**
     * Generates documentation objects for the given scopes.
     *
     * @param scopes Map with scopes' names as keys and their settings as values, see {@link InstrumentationSettings#getScopes()}.
     *
     * @return Returns a List with the generated {@link BaseDocs} objects.
     */
    private List<BaseDocs> generateScopeDocs(Map<String, InstrumentationScopeSettings> scopes) {
        List<BaseDocs> scopeDocs = new ArrayList<>();
        for (String scopeName : scopes.keySet()) {
            InstrumentationScopeSettings scopeSettings = scopes.get(scopeName);
            BaseDocumentation doc = scopeSettings.getDocs();

            String description = "";
            String since = "";
            if (doc != null) {
                description = doc.getDescription();
                since = doc.getSince();
            }
            scopeDocs.add(new BaseDocs(scopeName, description, since));
        }
        return scopeDocs;
    }

    /**
     * Generates documentation objects for the given metrics.
     *
     * @param metrics Map with metrics definitions' names as keys and their settings as values, see {@link MetricsSettings#getDefinitions()} ()}.
     *
     * @return Returns a List with the generated {@link MetricDocs} objects.
     */
    private List<MetricDocs> generateMetricDocs(Map<String, MetricDefinitionSettings> metrics) {

        List<MetricDocs> metricDocs = new ArrayList<>();
        for (String metricName : metrics.keySet()) {

            MetricDefinitionSettings metricDefinitionSettings = metrics.get(metricName);

            String description = metricDefinitionSettings.getDescription();
            if (description == null) {
                description = "";
            }
            String unit = metricDefinitionSettings.getUnit();

            metricDocs.add(new MetricDocs(metricName, description, unit));

        }
        return metricDocs;
    }

    /**
     * Generates documentation objects for the given actions.
     *
     * @param actions Map with actions' names as keys and their settings as values, see {@link InstrumentationSettings#getActions()} ()} ()}.
     *
     * @return Returns a List with the generated {@link ActionDocs} objects.
     */
    private List<ActionDocs> generateActionDocs(Map<String, GenericActionSettings> actions) {
        List<ActionDocs> actionDocs = new ArrayList<>();
        for (String actionName : actions.keySet()) {
            GenericActionSettings actionSettings = actions.get(actionName);
            ActionDocumentation doc = actionSettings.getDocs();

            String description = "";
            String since = "";
            String returnDesc = "";

            Map<String, String> inputDescriptions = Collections.emptyMap();

            if (doc != null) {
                description = doc.getDescription();
                since = doc.getSince();
                returnDesc = doc.getReturnValue();
                inputDescriptions = doc.getInputs();
            }

            Boolean isVoid = actionSettings.getIsVoid();

            List<ActionInputDocs> inputs = new ArrayList<>();
            Map<String, String> inputTypes = actionSettings.getInput();

            for (String inputName : inputTypes.keySet()) {
                inputs.add(new ActionInputDocs(inputName, inputTypes.get(inputName), inputDescriptions.getOrDefault(inputName, "")));
            }

            actionDocs.add(new ActionDocs(actionName, description, since, inputs, returnDesc, isVoid));
        }
        return actionDocs;
    }

    /**
     * Generates documentation objects for the given actions. Other than the other similar methods in this class,
     * the RuleDocs objects are returned in a map, because that map is needed to later call {@link RuleDocs#addActionCallsFromIncludedRules(Map, List)}.
     *
     * @param rules Map with rules' names as keys and their settings as values, see {@link InstrumentationSettings#getRules()}.
     *
     * @return Returns a Map with the generated {@link RuleDocs} as values and their names as keys.
     */
    private Map<String, RuleDocs> generateRuleDocsMap(Map<String, InstrumentationRuleSettings> rules) {
        Map<String, RuleDocs> ruleDocsMap = new HashMap<>();
        for (String ruleName : rules.keySet()) {

            InstrumentationRuleSettings ruleSettings = rules.get(ruleName);
            BaseDocumentation doc = ruleSettings.getDocs();

            String description = "";
            String since = "";
            if (doc != null) {
                description = doc.getDescription();
                since = doc.getSince();
            }

            Map<String, Boolean> include = ruleSettings.getInclude();
            List<String> includeForDoc = include.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            Map<String, Boolean> scopes = ruleSettings.getScopes();
            List<String> scopesForDoc = scopes.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<RuleMetricsDocs> ruleMetricsDocs = generateRuleMetricsDocs(ruleSettings);

            RuleTracingDocs ruleTracingDocs = generateRuleTracingDocs(ruleSettings);

            Map<String, Map<String, ActionCallDocs>> actionCallsMap = generateActionCallDocs(ruleSettings);

            ruleDocsMap.put(ruleName, new RuleDocs(ruleName, description, since, includeForDoc, scopesForDoc, ruleMetricsDocs, ruleTracingDocs, actionCallsMap));
        }
        return ruleDocsMap;
    }

    /**
     * Generates documentation objects for the {@link MetricRecordingSettings} of a rule.
     *
     * @param ruleSettings The rule's {@link InstrumentationRuleSettings} object.
     *
     * @return Returns a List with the generated {@link RuleMetricsDocs} objects.
     */
    private List<RuleMetricsDocs> generateRuleMetricsDocs(InstrumentationRuleSettings ruleSettings) {
        List<RuleMetricsDocs> metricsDocs = new ArrayList<>();
        for (String metricKey : ruleSettings.getMetrics().keySet()) {
            MetricRecordingSettings currentMetric = ruleSettings.getMetrics().get(metricKey);

            String metricName;
            if (currentMetric.getMetric() != null) {
                metricName = currentMetric.getMetric();
            } else {
                metricName = metricKey;
            }
            String value = currentMetric.getValue();
            Map<String, String> dataTags = currentMetric.getDataTags();
            Map<String, String> constantTags = currentMetric.getConstantTags();

            metricsDocs.add(new RuleMetricsDocs(metricName, value, dataTags, constantTags));
        }
        return metricsDocs;
    }

    /**
     * Generates documentation objects for the {@link RuleTracingSettings} of a rule.
     *
     * @param ruleSettings The rule's {@link InstrumentationRuleSettings} object.
     *
     * @return Returns a List with the generated {@link RuleTracingDocs} objects.
     */
    private RuleTracingDocs generateRuleTracingDocs(InstrumentationRuleSettings ruleSettings) {
        RuleTracingSettings tracingSettings = ruleSettings.getTracing();
        RuleTracingDocs ruleTracingDocs = null;
        if (tracingSettings != null) {
            Boolean startSpan = tracingSettings.getStartSpan();

            Map<String, String> startSpanConditions = new HashMap<>();
            ConditionalActionSettings conditionalActionSettings = tracingSettings.getStartSpanConditions();

            for (Field field : conditionalActionSettings.getClass().getDeclaredFields()) {

                if (!field.isSynthetic()) {
                    String fieldName = field.getName();
                    try {
                        String fieldValue = BeanUtils.getProperty(conditionalActionSettings, fieldName);
                        if (fieldValue != null) {
                            startSpanConditions.put(fieldName, fieldValue);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Map<String, String> attributes = tracingSettings.getAttributes();
            ruleTracingDocs = new RuleTracingDocs(startSpan, startSpanConditions, attributes);
        }
        return ruleTracingDocs;
    }

    /**
     * Generates documentation objects for the actions to be executed in a rule's entry, exit, etc. state,
     * see for example {@link InstrumentationRuleSettings#getEntry()}.
     *
     * @param ruleSettings The rule's {@link InstrumentationRuleSettings} object.
     *
     * @return Returns a Map with the field names, e.g. "entry" or "exit" as key, and another Map with
     * the names of {@link ActionCallSettings} as keys and the corresponding {@link ActionCallDocs} as values.
     */
    private Map<String, Map<String, ActionCallDocs>> generateActionCallDocs(InstrumentationRuleSettings ruleSettings) {
        Map<String, Map<String, ActionCallDocs>> actionCallsMap = new HashMap<>();
        for (RuleLifecycleState ruleLifecycleState : RuleLifecycleState.values()) {
            try {
                Map<String, ActionCallSettings> singleStateActionCallMap = (Map<String, ActionCallSettings>) PropertyUtils.getProperty(ruleSettings, ruleLifecycleState.getKey());
                Map<String, ActionCallDocs> actionCallDocs = new TreeMap<>();
                for (String actionCallKey : singleStateActionCallMap.keySet()) {
                    actionCallDocs.put(actionCallKey, new ActionCallDocs(actionCallKey, singleStateActionCallMap.get(actionCallKey)
                            .getAction()));
                }
                actionCallsMap.put(ruleLifecycleState.getKey(), actionCallDocs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return actionCallsMap;
    }

}
