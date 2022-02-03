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

        for(String includedRuleName: includedRules){

            RuleDoc includedRule = allRuleDocs.get(includedRuleName);
            if(includedRule != null){
                Map<String, Map<String, RuleActionCallDoc>> includedRuleEntryExits = includedRule.getEntryExits();

                for(String includedRuleEntryExitKey: includedRuleEntryExits.keySet()){

                    if(!entryExits.containsKey(includedRuleEntryExitKey)){
                        entryExits.put(includedRuleEntryExitKey, new TreeMap<>());
                    }
                    for(RuleActionCallDoc entryExitActionCall: includedRuleEntryExits.get(includedRuleEntryExitKey).values()){

                        Map<String, RuleActionCallDoc> actionCallDocs = entryExits.get(includedRuleEntryExitKey);

                        if(!actionCallDocs.containsKey(entryExitActionCall.getName())) {
                            actionCallDocs.put(entryExitActionCall.getName(), new RuleActionCallDoc(entryExitActionCall,
                                    includedRule.getName()));
                        }
                    }
                }
                addEntryExitFromIncludedRules(allRuleDocs, includedRule.getInclude());
            }
        }
    }
}
