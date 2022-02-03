package inspectit.ocelot.config.doc.generator.docobjects;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleDocTest {
    
    private static final String RULE_1_NAME = "testRule1";
    private static final String RULE_2_NAME = "testRule2";
    private static final String RULE_3_NAME = "testRule3";
    private static final String RULE_4_NAME = "testRule4";

    private static final String ACTION_FROM_RULE_1 = "a_1";
    private static final String ACTION_FROM_RULE_2 = "a_2";
    private static final String ACTION_FROM_RULE_3 = "a_3";
    private static final String ACTION_FROM_RULE_4 = "a_4";
    
    private static final String OVERWRITTEN_BY_1 = "overwritten_by_1";
    private static final String OVERWRITTEN_BY_2 = "overwritten_by_2";
    private static final String OVERWRITTEN_BY_1_AND_2 = "overwritten_by_1_and_2";
    private static final String CREATED_BY_3 = "created_by_3";
    private static final String CREATED_BY_4 = "created_by_4";

    private static final String ENTRY_KEY = "entry";
    private static final String EXIT_KEY = "exit";

    private Map<String, RuleDoc> getTestRuleDocs(){
        Map<String, RuleDoc> allRuleDocs = new HashMap<>();

        /* 
        RuleDoc1: inherits from RuleDoc2 and 4. Overwrites ActionCall overwritten_by_1 and overwritten_by_1_and_2 
        which are both also in RuleDoc2.
         */
        allRuleDocs.put(RULE_1_NAME, new RuleDoc(RULE_1_NAME));
        List<String> includes1 = new ArrayList<>();
        includes1.add(RULE_2_NAME);
        includes1.add(RULE_4_NAME);
        allRuleDocs.get(RULE_1_NAME).setInclude(includes1);

        Map<String, Map<String, RuleActionCallDoc>> entryExits1 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls1 = new HashMap<>();
        RuleActionCallDoc actionCall11 = new RuleActionCallDoc(OVERWRITTEN_BY_1, ACTION_FROM_RULE_1);
        actionCalls1.put(OVERWRITTEN_BY_1, actionCall11);
        RuleActionCallDoc actionCall12 = new RuleActionCallDoc(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_1);
        actionCalls1.put(OVERWRITTEN_BY_1_AND_2, actionCall12);
        entryExits1.put(ENTRY_KEY, actionCalls1);
        allRuleDocs.get(RULE_1_NAME).setEntryExits(entryExits1);

        /*
        RuleDoc2: inherits from RuleDoc3. Overwrites overwritten_by_1_and_2 from RuleDoc3 and creates own ActionCall
        overwritten_by_1.
        */
        allRuleDocs.put(RULE_2_NAME, new RuleDoc(RULE_2_NAME));
        List<String> includes2 = new ArrayList<>();
        includes2.add(RULE_3_NAME);
        allRuleDocs.get(RULE_2_NAME).setInclude(includes2);
        
        Map<String, Map<String, RuleActionCallDoc>> entryExits2 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls2 = new HashMap<>();
        RuleActionCallDoc actionCall21 = new RuleActionCallDoc(OVERWRITTEN_BY_1, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_1, actionCall21);
        RuleActionCallDoc actionCall22 = new RuleActionCallDoc(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_1_AND_2, actionCall22);
        RuleActionCallDoc actionCall23 = new RuleActionCallDoc(OVERWRITTEN_BY_2, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_2, actionCall23);
        entryExits2.put(ENTRY_KEY, actionCalls2);
        allRuleDocs.get(RULE_2_NAME).setEntryExits(entryExits2);
        
        /*
        RuleDoc3: Creates ActionCalls overwritten_by_1_and_2, overwritten_by_2 and created_by_3.
        */
        allRuleDocs.put(RULE_3_NAME, new RuleDoc(RULE_3_NAME));
        Map<String, Map<String, RuleActionCallDoc>> entryExits3 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls3 = new HashMap<>();
        RuleActionCallDoc actionCall31 = new RuleActionCallDoc(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_3);
        actionCalls3.put(OVERWRITTEN_BY_1_AND_2, actionCall31);
        RuleActionCallDoc actionCall32 = new RuleActionCallDoc(CREATED_BY_3, ACTION_FROM_RULE_3);
        actionCalls3.put(CREATED_BY_3, actionCall32);
        RuleActionCallDoc actionCall33 = new RuleActionCallDoc(OVERWRITTEN_BY_2, ACTION_FROM_RULE_3);
        actionCalls3.put(OVERWRITTEN_BY_2, actionCall33);
        entryExits3.put(ENTRY_KEY, actionCalls3);
        allRuleDocs.get(RULE_3_NAME).setEntryExits(entryExits3);

        /*
        RuleDoc4: Creates ActionCall created_by_4.
        */
        allRuleDocs.put(RULE_4_NAME, new RuleDoc(RULE_4_NAME));
        Map<String, Map<String, RuleActionCallDoc>> entryExits4 = new HashMap<>();
        Map<String, RuleActionCallDoc> actionCalls4 = new HashMap<>();
        RuleActionCallDoc actionCall41 = new RuleActionCallDoc(CREATED_BY_4, ACTION_FROM_RULE_4);
        actionCalls4.put(CREATED_BY_4, actionCall41);
        entryExits4.put(EXIT_KEY, actionCalls4);
        allRuleDocs.get(RULE_4_NAME).setEntryExits(entryExits4);

        return allRuleDocs;
    }

    @Nested
    class AddEntryExitFromIncludedRulesTest{

        @Test
        void addEntryExitFromIncludedRules() {

            Map<String, RuleDoc> result = getTestRuleDocs();
            for(RuleDoc currentRule: result.values()){
                currentRule.addEntryExitFromIncludedRules(result, currentRule.getInclude());
            }

            Map<String, RuleDoc> expected = getTestRuleDocs();

            // ActionCall created in 3 is added to testRule2 and testRule1
            RuleActionCallDoc created_by_3 = expected.get(RULE_3_NAME).getEntryExits().get(ENTRY_KEY).get(CREATED_BY_3);
            RuleActionCallDoc inherited_from_3 = new RuleActionCallDoc(created_by_3, RULE_3_NAME);
            expected.get(RULE_2_NAME).getEntryExits().get(ENTRY_KEY).put(CREATED_BY_3, inherited_from_3);
            expected.get(RULE_1_NAME).getEntryExits().get(ENTRY_KEY).put(CREATED_BY_3, inherited_from_3);

            // ActionCall created in 3 but overwritten in 2 is added to testRule1
            RuleActionCallDoc overwritten_by_2 = expected.get(RULE_2_NAME).getEntryExits().get(ENTRY_KEY).get(OVERWRITTEN_BY_2);
            RuleActionCallDoc inherited_from_2 = new RuleActionCallDoc(overwritten_by_2, RULE_2_NAME);
            expected.get(RULE_1_NAME).getEntryExits().get(ENTRY_KEY).put(OVERWRITTEN_BY_2, inherited_from_2);

            // ActionCall created in 4 is added to testRule1
            RuleActionCallDoc created_by_4 = expected.get(RULE_4_NAME).getEntryExits().get(EXIT_KEY).get(CREATED_BY_4);
            RuleActionCallDoc inherited_from_4 = new RuleActionCallDoc(created_by_4, RULE_4_NAME);
            Map<String, RuleActionCallDoc> actionCallsEntry = new HashMap<>();
            actionCallsEntry.put(CREATED_BY_4, inherited_from_4);
            expected.get(RULE_1_NAME).getEntryExits().put(EXIT_KEY, actionCallsEntry);

            assertEquals(expected, result);
        }
        
    }
}