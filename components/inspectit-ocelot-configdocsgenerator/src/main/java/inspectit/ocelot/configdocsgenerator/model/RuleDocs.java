package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import lombok.Setter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data container for documentation of a single Rule's {@link InstrumentationRuleSettings} in Config Documentation.
 */
@Getter
@Setter
public class RuleDocs extends BaseDocs {

    /**
     * List of rules that are included in the documented rule, see {@link InstrumentationRuleSettings#getInclude()}.
     */
    private List<String> include;

    /**
     * List of scopes that are used by the documented rule, see {@link InstrumentationRuleSettings#getScopes()}.
     */
    private List<String> scopes;

    /**
     * Documentation info for the Rule's metrics settings (see {@link RuleMetricsDocs} and {@link InstrumentationRuleSettings#getMetrics()}).
     */
    private List<RuleMetricsDocs> metricsDocs;

    /**
     * Documentation info for the Rule's tracing settings (see {@link RuleTracingDocs} and {@link InstrumentationRuleSettings#getTracing()}).
     */
    private RuleTracingDocs tracingDoc;

    /**
     * Documentation info for the actions to be executed in the rule's different lifecycle states,
     * see for example {@link InstrumentationRuleSettings#getEntry()}.
     * Keys are the corresponding field names, see {@link RuleLifecycleState}.
     * Values are another Map with the {@link rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings}'s name as key and its corresponding {@link ActionCallDocs} object as value.
     */
    private Map<String, Map<String, ActionCallDocs>> actionCallsMap;

    public RuleDocs(String name, String description, String since, List<String> include, List<String> scopes,
                    List<RuleMetricsDocs> metricsDocs, RuleTracingDocs tracingDoc, Map<String, Map<String,
                    ActionCallDocs>> actionCallsMap, Set<String> files) {
        super(name, description, since, files);
        this.include = include;
        this.scopes = scopes;
        this.metricsDocs = metricsDocs;
        this.tracingDoc = tracingDoc;
        this.actionCallsMap = actionCallsMap;
    }

    /**
     * Adds entry and exit attributes to the current rule from its included rules.
     * Attributes that the current rule has overwritten will not be added a second time.
     * Also adds attributes from rules that are included by the current rule's included rules by calling itself
     * recursively.
     *
     * @param allRuleDocs   Map that contains all RuleDocs with their names as keys.
     * @param includedRules List of the included rules that should be added. In the original this could also just
     *                      be replaced by the attribute {@link RuleDocs#include}, however for recursive calls it
     *                      is needed as a parameter.
     */
    public void addActionCallsFromIncludedRules(Map<String, RuleDocs> allRuleDocs, List<String> includedRules) {

        // Iterate through all included Rules' RuleDocs
        includedRules.stream().map(allRuleDocs::get).filter(Objects::nonNull).forEach(includedRule -> {

            // In each RuleDoc iterate through its entries in actionCallsMap,
            // i. e. entry, exit, pre-entry, pre-exit, etc.
            includedRule.getActionCallsMap().forEach((includedACMKey, includedACMValue) -> {

                // Iterate through the entries in one actionCallsMap entry, i. e. the attributes defined for this state.
                includedACMValue.entrySet().stream()
                        // If an attribute already is in the actionCallsMap of the original Rule, that means the
                        // original Rule has overwritten the attribute and the attribute from the included Rule
                        // does not need to be added.
                        .filter(actionCallEntry -> !actionCallsMap.get(includedACMKey)
                                .containsKey(actionCallEntry.getKey()))
                        // Add the remaining attributes from the included Rule to the original Rule with a reference
                        // to the included Rule to know where it came from.
                        .forEach(actionCallEntry -> {
                            actionCallsMap.get(includedACMKey)
                                    .put(actionCallEntry.getKey(), new ActionCallDocs(actionCallEntry.getValue(), includedRule.getName()));
                        });
            });
            addActionCallsFromIncludedRules(allRuleDocs, includedRule.getInclude());
        });
    }
}
