package rocks.inspectit.ocelot.core.instrumentation.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RuleDependencyTreePrinterTest {

    private List<InstrumentationRule> generateRules() {
        InstrumentationScope dummyScope = new InstrumentationScope(null, null);

        // @formatter:off
        List<InstrumentationRule> rules = Arrays.asList(
                InstrumentationRule.builder()
                        .name("rule_a")
                        .includedRuleName("rule_b")
                        .scope(dummyScope)
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_b")
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_c")
                        .includedRuleName("rule_b")
                        .includedRuleName("rule_d")
                        .scope(dummyScope)
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_d")
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_e")
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_f")
                        .includedRuleName("rule_e")
                        .includedRuleName("rule_g")
                        .build(),
                InstrumentationRule.builder()
                        .name("rule_g")
                        .build()
        );
        // @formatter:on

        return rules;
    }

    @Nested
    public class ToRuleEntry {

        @Test
        public void toRuleEntry() {
            List<InstrumentationRule> rules = generateRules();

            RuleDependencyTreePrinter tree = new RuleDependencyTreePrinter(rules);

            RuleDependencyTreePrinter.RuleEntry result = tree.toRuleEntry(rules.get(0), true);

            assertThat(result.getName()).isEqualTo("rule_a");
            assertThat(result.getChildren()).extracting(RuleDependencyTreePrinter.RuleEntry::getName, RuleDependencyTreePrinter.RuleEntry::getChildren)
                    .containsExactly(tuple("rule_b", Collections.emptyList()));
        }
    }

    @Nested
    public class GetScopedRules {

        @Test
        public void getScopedRules() {
            RuleDependencyTreePrinter tree = new RuleDependencyTreePrinter(generateRules());

            List<InstrumentationRule> rules = tree.getScopedRules();

            assertThat(rules).extracting(InstrumentationRule::getName).containsExactly("rule_a", "rule_c");
        }
    }

    @Nested
    public class GetRuleByName {

        @Test
        public void ruleExists() {
            RuleDependencyTreePrinter tree = new RuleDependencyTreePrinter(generateRules());

            InstrumentationRule rule = tree.getRuleByName("rule_a");

            assertThat(rule.getName()).isEqualTo("rule_a");
        }

        @Test
        public void ruleDoesNotExist() {
            RuleDependencyTreePrinter tree = new RuleDependencyTreePrinter(generateRules());

            InstrumentationRule rule = tree.getRuleByName("not_existing");

            assertThat(rule).isNull();
        }
    }

    @Nested
    public class ToString {

        @Test
        public void printTree() {
            RuleDependencyTreePrinter tree = new RuleDependencyTreePrinter(generateRules());

            // @formatter:off
            assertThat(tree.toString()).isEqualTo(
                    "------------------------------------------------------------\n" +
                    "Rule Dependency-Tree\n" +
                    "------------------------------------------------------------\n" +
                    "+--- rule_a\n" +
                    "|    \\--- rule_b\n" +
                    "+--- rule_c\n" +
                    "|    +--- rule_b\n" +
                    "|    \\--- rule_d\n" +
                    "\\--- <UNUSED> rule_f\n" +
                    "     +--- rule_e\n"+
                    "     \\--- rule_g\n"
            );
            // @formatter:on
        }
    }
}