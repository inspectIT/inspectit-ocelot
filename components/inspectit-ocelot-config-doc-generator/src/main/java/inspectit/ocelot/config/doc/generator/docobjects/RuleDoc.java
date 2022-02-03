package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@EqualsAndHashCode (callSuper = true)
public class RuleDoc extends BaseDoc {

    public RuleDoc(String name, String description, List<String> include, List<String> scopes,
                   List<RuleMetricsDoc> metricsDocs, RuleTracingDoc tracingDoc, Map<String, Map<String,
            RuleActionCallDoc>> entryExits) {
        super(name, description);
        this.include = include;
        this.scopes = scopes;
        this.metricsDocs = metricsDocs;
        this.tracingDoc = tracingDoc;
        this.entryExits = entryExits;
    }

    /*
    As of now only needed for simpler code in RuleDocTest.
     */
    public RuleDoc(String name){
        super(name, "");
    }

    private List<String> include = Collections.emptyList();
    private List<String> scopes = Collections.emptyList();
    private List<RuleMetricsDoc> metricsDocs = Collections.emptyList();
    private RuleTracingDoc tracingDoc;
    private Map<String, Map<String, RuleActionCallDoc>> entryExits = Collections.emptyMap();

    public void addEntryExitFromIncludedRules(Map<String, RuleDoc> allRuleDocs, List<String> includedRules){

        // Iterate through all included Rules' RuleDocs
        includedRules.stream().map(allRuleDocs::get).filter(Objects::nonNull).forEach(includedRule -> {

            // In each RuleDoc iterate through its entries in entryExits,
            // i. e. entry, exit, pre-entry, pre-exit, etc.
            includedRule.getEntryExits().forEach((includedEEKey, includedEnExValue) -> {
                // If the original Rule does not have a key in entryExits that an included Rule has, an empty TreeMap
                // is added at that point for future usage.
                if (!entryExits.containsKey(includedEEKey)) {
                    entryExits.put(includedEEKey, new TreeMap<>());
                }

                // Iterate through the entries in one entryExit entry, i. e. the attributes defined for this state.
                includedEnExValue.entrySet()
                        .stream()
                        // If an attribute already is in the entryExits Map of the original Rule, that means the
                        // original Rule has overwritten the attribute and the attribute from the included Rule
                        // does not need to be added.
                        .filter(actionCallEntry -> !entryExits.get(includedEEKey)
                                .containsKey(actionCallEntry.getKey()))
                        // Add the remaining attributes from the included Rule to the original Rule with a reference
                        // to the included Rule to know where it came from.
                        .forEach(actionCallEntry -> {
                            entryExits.get(includedEEKey)
                                    .put(actionCallEntry.getKey(), new RuleActionCallDoc(
                                            actionCallEntry.getValue(), includedRule.getName()));
                        });
            });
            addEntryExitFromIncludedRules(allRuleDocs, includedRule.getInclude());
        });
    }
}
