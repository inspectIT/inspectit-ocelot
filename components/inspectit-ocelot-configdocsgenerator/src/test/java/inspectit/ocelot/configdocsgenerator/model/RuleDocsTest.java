package inspectit.ocelot.configdocsgenerator.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RuleDocsTest {

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

    /**
     * Generate an entryExits Map for a {@link RuleDocs} object with empty TreeMaps as values.
     *
     * @return The entryExits Map with empty TreeMaps.
     */
    public static Map<String, Map<String, ActionCallDocs>> getEmptyEntryExits() {
        Map<String, Map<String, ActionCallDocs>> entryExits = new HashMap<>();
        for (RuleDocs.EntryExitKey entryExitKey : RuleDocs.EntryExitKey.values()) {
            entryExits.put(entryExitKey.getKey(), new TreeMap<>());
        }
        return entryExits;
    }

    /**
     * Helper function to simplify creating RuleDocs for testing purposes.
     *
     * @param name Name of the rule.
     *
     * @return RuleDocs object with the given name and otherwise empty values.
     */
    private RuleDocs simpleRuleDoc(String name) {
        return new RuleDocs(name, "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, Collections.emptyMap());
    }

    /**
     * Generate the {@link RuleDocs} objects to use for testing.
     *
     * @return Map with {@link RuleDocs} objects as values and their names as keys.
     */
    private Map<String, RuleDocs> getTestRuleDocs() {
        Map<String, RuleDocs> allRuleDocs = new HashMap<>();

        /* 
        RuleDoc1: inherits from RuleDoc2 and 4. Overwrites ActionCall overwritten_by_1 and overwritten_by_1_and_2 
        which are both also in RuleDoc2.
         */
        allRuleDocs.put(RULE_1_NAME, simpleRuleDoc(RULE_1_NAME));
        List<String> includes1 = new ArrayList<>();
        includes1.add(RULE_2_NAME);
        includes1.add(RULE_4_NAME);
        allRuleDocs.get(RULE_1_NAME).setInclude(includes1);

        Map<String, Map<String, ActionCallDocs>> entryExits1 = getEmptyEntryExits();
        Map<String, ActionCallDocs> actionCalls1 = new HashMap<>();
        ActionCallDocs actionCall11 = new ActionCallDocs(OVERWRITTEN_BY_1, ACTION_FROM_RULE_1);
        actionCalls1.put(OVERWRITTEN_BY_1, actionCall11);
        ActionCallDocs actionCall12 = new ActionCallDocs(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_1);
        actionCalls1.put(OVERWRITTEN_BY_1_AND_2, actionCall12);
        entryExits1.put(ENTRY_KEY, actionCalls1);
        allRuleDocs.get(RULE_1_NAME).setEntryExits(entryExits1);

        /*
        RuleDoc2: inherits from RuleDoc3. Overwrites overwritten_by_1_and_2 from RuleDoc3 and creates own ActionCall
        overwritten_by_1.
        */
        allRuleDocs.put(RULE_2_NAME, simpleRuleDoc(RULE_2_NAME));
        List<String> includes2 = new ArrayList<>();
        includes2.add(RULE_3_NAME);
        allRuleDocs.get(RULE_2_NAME).setInclude(includes2);

        Map<String, Map<String, ActionCallDocs>> entryExits2 = new HashMap<>();
        Map<String, ActionCallDocs> actionCalls2 = new HashMap<>();
        ActionCallDocs actionCall21 = new ActionCallDocs(OVERWRITTEN_BY_1, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_1, actionCall21);
        ActionCallDocs actionCall22 = new ActionCallDocs(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_1_AND_2, actionCall22);
        ActionCallDocs actionCall23 = new ActionCallDocs(OVERWRITTEN_BY_2, ACTION_FROM_RULE_2);
        actionCalls2.put(OVERWRITTEN_BY_2, actionCall23);
        entryExits2.put(ENTRY_KEY, actionCalls2);
        allRuleDocs.get(RULE_2_NAME).setEntryExits(entryExits2);
        
        /*
        RuleDoc3: Creates ActionCalls overwritten_by_1_and_2, overwritten_by_2 and created_by_3.
        */
        allRuleDocs.put(RULE_3_NAME, simpleRuleDoc(RULE_3_NAME));
        Map<String, Map<String, ActionCallDocs>> entryExits3 = new HashMap<>();
        Map<String, ActionCallDocs> actionCalls3 = new HashMap<>();
        ActionCallDocs actionCall31 = new ActionCallDocs(OVERWRITTEN_BY_1_AND_2, ACTION_FROM_RULE_3);
        actionCalls3.put(OVERWRITTEN_BY_1_AND_2, actionCall31);
        ActionCallDocs actionCall32 = new ActionCallDocs(CREATED_BY_3, ACTION_FROM_RULE_3);
        actionCalls3.put(CREATED_BY_3, actionCall32);
        ActionCallDocs actionCall33 = new ActionCallDocs(OVERWRITTEN_BY_2, ACTION_FROM_RULE_3);
        actionCalls3.put(OVERWRITTEN_BY_2, actionCall33);
        entryExits3.put(ENTRY_KEY, actionCalls3);
        allRuleDocs.get(RULE_3_NAME).setEntryExits(entryExits3);

        /*
        RuleDoc4: Creates ActionCall created_by_4.
        */
        allRuleDocs.put(RULE_4_NAME, simpleRuleDoc(RULE_4_NAME));
        Map<String, Map<String, ActionCallDocs>> entryExits4 = new HashMap<>();
        Map<String, ActionCallDocs> actionCalls4 = new HashMap<>();
        ActionCallDocs actionCall41 = new ActionCallDocs(CREATED_BY_4, ACTION_FROM_RULE_4);
        actionCalls4.put(CREATED_BY_4, actionCall41);
        entryExits4.put(EXIT_KEY, actionCalls4);
        allRuleDocs.get(RULE_4_NAME).setEntryExits(entryExits4);

        return allRuleDocs;
    }

    @Nested
    class AddEntryExitFromIncludedRulesTest {

        @Test
        void addEntryExitFromIncludedRules() {

            Map<String, RuleDocs> result = getTestRuleDocs();
            for (RuleDocs currentRule : result.values()) {
                currentRule.addEntryExitFromIncludedRules(result, currentRule.getInclude());
            }

            Map<String, RuleDocs> expected = getTestRuleDocs();

            // ActionCall created in 3 is added to testRule2 and testRule1
            ActionCallDocs created_by_3 = expected.get(RULE_3_NAME).getEntryExits().get(ENTRY_KEY).get(CREATED_BY_3);

            ActionCallDocs inherited_from_3 = Mockito.mock(ActionCallDocs.class);
            when(inherited_from_3.getName()).thenReturn(created_by_3.getName());
            when(inherited_from_3.getActionName()).thenReturn(created_by_3.getActionName());
            when(inherited_from_3.getInheritedFrom()).thenReturn(RULE_3_NAME);

            expected.get(RULE_2_NAME).getEntryExits().get(ENTRY_KEY).put(CREATED_BY_3, inherited_from_3);
            expected.get(RULE_1_NAME).getEntryExits().get(ENTRY_KEY).put(CREATED_BY_3, inherited_from_3);

            // ActionCall created in 3 but overwritten in 2 is added to testRule1
            ActionCallDocs overwritten_by_2 = expected.get(RULE_2_NAME)
                    .getEntryExits()
                    .get(ENTRY_KEY)
                    .get(OVERWRITTEN_BY_2);

            ActionCallDocs inherited_from_2 = Mockito.mock(ActionCallDocs.class);
            when(inherited_from_2.getName()).thenReturn(overwritten_by_2.getName());
            when(inherited_from_2.getActionName()).thenReturn(overwritten_by_2.getActionName());
            when(inherited_from_2.getInheritedFrom()).thenReturn(RULE_2_NAME);

            expected.get(RULE_1_NAME).getEntryExits().get(ENTRY_KEY).put(OVERWRITTEN_BY_2, inherited_from_2);

            // ActionCall created in 4 is added to testRule1
            ActionCallDocs created_by_4 = expected.get(RULE_4_NAME).getEntryExits().get(EXIT_KEY).get(CREATED_BY_4);

            ActionCallDocs inherited_from_4 = Mockito.mock(ActionCallDocs.class);
            when(inherited_from_4.getName()).thenReturn(created_by_4.getName());
            when(inherited_from_4.getActionName()).thenReturn(created_by_4.getActionName());
            when(inherited_from_4.getInheritedFrom()).thenReturn(RULE_4_NAME);

            Map<String, ActionCallDocs> actionCallsEntry = new HashMap<>();
            actionCallsEntry.put(CREATED_BY_4, inherited_from_4);
            expected.get(RULE_1_NAME).getEntryExits().put(EXIT_KEY, actionCallsEntry);

            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }

    }
}