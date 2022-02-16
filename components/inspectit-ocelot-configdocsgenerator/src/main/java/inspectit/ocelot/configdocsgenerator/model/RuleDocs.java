package inspectit.ocelot.configdocsgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Documentation info for the actions to be executed in the rule's entry, exit, etc. state,
     * see for example {@link InstrumentationRuleSettings#getEntry()}.
     * Keys are the corresponding field names, see {@link EntryExitKey}.
     * Values are another Map with the {@link rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings}'s name as key and its corresponding {@link ActionCallDocs} object as value.
     */
    private Map<String, Map<String, ActionCallDocs>> entryExits;

    public RuleDocs(String name, String description, String since, List<String> include, List<String> scopes, List<RuleMetricsDocs> metricsDocs, RuleTracingDocs tracingDoc, Map<String, Map<String, ActionCallDocs>> entryExits) {
        super(name, description, since);
        this.include = include;
        this.scopes = scopes;
        this.metricsDocs = metricsDocs;
        this.tracingDoc = tracingDoc;
        this.entryExits = entryExits;
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
    public void addEntryExitFromIncludedRules(Map<String, RuleDocs> allRuleDocs, List<String> includedRules) {

        // Iterate through all included Rules' RuleDocs
        includedRules.stream().map(allRuleDocs::get).filter(Objects::nonNull).forEach(includedRule -> {

            // In each RuleDoc iterate through its entries in entryExits,
            // i. e. entry, exit, pre-entry, pre-exit, etc.
            includedRule.getEntryExits().forEach((includedEnExKey, includedEnExValue) -> {

                // Iterate through the entries in one entryExit entry, i. e. the attributes defined for this state.
                includedEnExValue.entrySet().stream()
                        // If an attribute already is in the entryExits Map of the original Rule, that means the
                        // original Rule has overwritten the attribute and the attribute from the included Rule
                        // does not need to be added.
                        .filter(actionCallEntry -> !entryExits.get(includedEnExKey)
                                .containsKey(actionCallEntry.getKey()))
                        // Add the remaining attributes from the included Rule to the original Rule with a reference
                        // to the included Rule to know where it came from.
                        .forEach(actionCallEntry -> {
                            entryExits.get(includedEnExKey)
                                    .put(actionCallEntry.getKey(), new ActionCallDocs(actionCallEntry.getValue(), includedRule.getName()));
                        });
            });
            addEntryExitFromIncludedRules(allRuleDocs, includedRule.getInclude());
        });
    }

    /**
     * Enum with possible field names in {@link InstrumentationRuleSettings} for entry, exit, etc. ActionCall settings.
     */
    @Getter
    @RequiredArgsConstructor
    public enum EntryExitKey {

        PRE_ENTRY("preEntry"), ENTRY("entry"), POST_ENTRY("postEntry"), PRE_EXIT("preExit"), EXIT("exit"), POST_EXIT("postExit");

        private final String key;
    }
}