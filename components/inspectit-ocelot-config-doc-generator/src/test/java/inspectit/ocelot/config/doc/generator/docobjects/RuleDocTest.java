package inspectit.ocelot.config.doc.generator.docobjects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

class RuleDocTest {

    private Map<String, RuleDoc> getTestRuleDocs(){
        Map<String, RuleDoc> allRuleDocs = new HashMap<>();

        /* 
        RuleDoc1: inherits from RuleDoc2 and 4. Overwrites ActionCall overwritten_by_1 and overwritten_by_1_and_2 
        which are both also in RuleDoc2.
         */
        allRuleDocs.put("testRule1", new RuleDoc("testRule1"));
        List<String> includes1 = new ArrayList<>();
        includes1.add("testRule2");
        includes1.add("testRule4");
        allRuleDocs.get("testRule1").setInclude(includes1);

        Map<String, Map<String, RuleActionCallDoc>> entryExits1 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls1 = new HashMap<>();
        RuleActionCallDoc actionCall11 = new RuleActionCallDoc("overwritten_by_1", "a_1");
        actionCalls1.put("overwritten_by_1", actionCall11);
        RuleActionCallDoc actionCall12 = new RuleActionCallDoc("overwritten_by_1_and_2", "a_1");
        actionCalls1.put("overwritten_by_1_and_2", actionCall12);
        entryExits1.put("entry", actionCalls1);
        allRuleDocs.get("testRule1").setEntryExits(entryExits1);

        /*
        RuleDoc2: inherits from RuleDoc3. Overwrites overwritten_by_1_and_2 from RuleDoc3 and creates own ActionCall
        overwritten_by_1.
        */
        allRuleDocs.put("testRule2", new RuleDoc("testRule2"));
        List<String> includes2 = new ArrayList<>();
        includes2.add("testRule3");
        allRuleDocs.get("testRule2").setInclude(includes2);
        
        Map<String, Map<String, RuleActionCallDoc>> entryExits2 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls2 = new HashMap<>();
        RuleActionCallDoc actionCall21 = new RuleActionCallDoc("overwritten_by_1", "a_2");
        actionCalls2.put("overwritten_by_1", actionCall21);
        RuleActionCallDoc actionCall22 = new RuleActionCallDoc("overwritten_by_1_and_2", "a_2");
        actionCalls2.put("overwritten_by_1_and_2", actionCall22);
        RuleActionCallDoc actionCall23 = new RuleActionCallDoc("overwritten_by_2", "a_2");
        actionCalls2.put("overwritten_by_2", actionCall23);
        entryExits2.put("entry", actionCalls2);
        allRuleDocs.get("testRule2").setEntryExits(entryExits2);
        
        /*
        RuleDoc3: Creates ActionCalls overwritten_by_1_and_2, overwritten_by_2 and created_by_3.
        */
        allRuleDocs.put("testRule3", new RuleDoc("testRule3"));
        Map<String, Map<String, RuleActionCallDoc>> entryExits3 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls3 = new HashMap<>();
        RuleActionCallDoc actionCall31 = new RuleActionCallDoc("overwritten_by_1_and_2", "a_3");
        actionCalls3.put("overwritten_by_1_and_2", actionCall31);
        RuleActionCallDoc actionCall32 = new RuleActionCallDoc("created_by_3", "a_3");
        actionCalls3.put("created_by_3", actionCall32);
        RuleActionCallDoc actionCall33 = new RuleActionCallDoc("overwritten_by_2", "a_3");
        actionCalls3.put("overwritten_by_2", actionCall33);
        entryExits3.put("entry", actionCalls3);
        allRuleDocs.get("testRule3").setEntryExits(entryExits3);

        /*
        RuleDoc4: Creates ActionCall created_by_4.
        */
        allRuleDocs.put("testRule4", new RuleDoc("testRule4"));
        Map<String, Map<String, RuleActionCallDoc>> entryExits4 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls4 = new HashMap<>();
        RuleActionCallDoc actionCall41 = new RuleActionCallDoc("created_by_4", "a_4");
        actionCalls4.put("created_by_4", actionCall41);
        entryExits4.put("exit", actionCalls4);
        allRuleDocs.get("testRule4").setEntryExits(entryExits4);

        return allRuleDocs;
    }
    
    @Test
    void addEntryExitFromIncludedRules() {

        Map<String, RuleDoc> result = getTestRuleDocs();
        for(RuleDoc currentRule: result.values()){
            currentRule.addEntryExitFromIncludedRules(result, currentRule.getInclude());
        }

        Map<String, RuleDoc> expected = getTestRuleDocs();

        // ActionCall created in 3 is added to testRule2 and testRule1
        RuleActionCallDoc created_by_3 = expected.get("testRule3").getEntryExits().get("entry").get("overwritten_by_2");
        RuleActionCallDoc inherited_from_3 = new RuleActionCallDoc(created_by_3, "testRule3");
        expected.get("testRule2").getEntryExits().get("entry").put("created_by_3", inherited_from_3);
        expected.get("testRule1").getEntryExits().get("entry").put("created_by_3", inherited_from_3);

        // ActionCall created in 3 but overwritten in 2 is added to testRule1
        RuleActionCallDoc overwritten_by_2 = expected.get("testRule2").getEntryExits().get("entry").get("overwritten_by_2");
        RuleActionCallDoc inherited_from_2 = new RuleActionCallDoc(overwritten_by_2, "testRule2");
        expected.get("testRule1").getEntryExits().get("entry").put("overwritten_by_2", inherited_from_2);

        // ActionCall created in 4 is added to testRule1
        RuleActionCallDoc created_by_4 = expected.get("testRule4").getEntryExits().get("exit").get("created_by_4");
        RuleActionCallDoc inherited_from_4 = new RuleActionCallDoc(created_by_4, "testRule4");
        Map<String, RuleActionCallDoc> actionCallsEntry = new HashMap<>();
        actionCallsEntry.put("created_by_4", inherited_from_4);
        expected.get("testRule1").getEntryExits().put("exit", actionCallsEntry);
    }
}