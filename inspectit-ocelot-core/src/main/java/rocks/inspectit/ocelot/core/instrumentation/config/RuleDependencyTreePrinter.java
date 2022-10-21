package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for generating a dependency tree of the given rules collection. This tree is just for debugging purposes
 * and, for example, can be logged.
 */
public class RuleDependencyTreePrinter {

    private final Collection<InstrumentationRule> rules;

    private final Set<String> usedRules = new HashSet<>();

    private final List<RuleEntry> ruleTree;

    /**
     * Constructor generating the dependency tree.
     *
     * @param rules the rules to consider in the tree
     */
    public RuleDependencyTreePrinter(Collection<InstrumentationRule> rules) {
        this.rules = rules;
        ruleTree = generateTree();
    }

    private List<RuleEntry> generateTree() {
        List<InstrumentationRule> scopedRules = getScopedRules();

        Stream<RuleEntry> usedRulesStream = scopedRules.stream().map(rule -> toRuleEntry(rule, true));

        List<RuleEntry> unusedRules = rules.stream()
                .filter(rule -> !this.usedRules.contains(rule.getName()))
                .map(rule -> toRuleEntry(rule, false))
                .collect(Collectors.toList());

        Stream<RuleEntry> unusedRulesStream = unusedRules.stream()
                .filter(entry -> !this.usedRules.contains(entry.getName()));

        return Stream.concat(usedRulesStream, unusedRulesStream)
                .sorted(Comparator.comparing(RuleEntry::getName))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    InstrumentationRule getRuleByName(String name) {
        return rules.stream().filter(rule -> rule.getName().equals(name)).findFirst().orElse(null);
    }

    @VisibleForTesting
    List<InstrumentationRule> getScopedRules() {
        return rules.stream().filter(rule -> !rule.getScopes().isEmpty()).collect(Collectors.toList());
    }

    @VisibleForTesting
    RuleEntry toRuleEntry(InstrumentationRule rule, boolean markAsUsed) {
        if (markAsUsed) {
            usedRules.add(rule.getName());
        }
        RuleEntry.RuleEntryBuilder builder = RuleEntry.builder()
                .name(rule.getName())
                .used(markAsUsed)
                .defaultRule(rule.isDefaultRule())
                .actionTracing(rule.isActionTracing());

        rule.getIncludedRuleNames()
                .stream()
                .map(this::getRuleByName)
                .filter(Objects::nonNull)
                .map(r -> toRuleEntry(r, true))
                .forEach(ruleEntry -> {
                    builder.child(ruleEntry);
                    usedRules.add(ruleEntry.getName());
                });

        return builder.build();
    }

    private void printRuleEntry(RuleEntry entry, StringBuilder builder, String prefix, boolean isLast) {
        builder.append(prefix);
        builder.append(isLast ? "\\" : "+");
        builder.append("--- ");
        if (!entry.isUsed()) {
            builder.append("<UNUSED> ");
        }
        if (entry.isDefaultRule()) {
            builder.append("*");
        }
        builder.append(entry.getName());
        if (entry.isActionTracing()) {
            builder.append(" (ACTIONTRACING)");
        }
        builder.append("\n");

        String newPrefix = prefix + (isLast ? " " : "|") + "    ";

        for (Iterator<RuleEntry> iterator = entry.getChildren().iterator(); iterator.hasNext(); ) {
            printRuleEntry(iterator.next(), builder, newPrefix, !iterator.hasNext());
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("------------------------------------------------------------\n");
        builder.append("Rule Dependency-Tree\n");
        builder.append("------------------------------------------------------------\n");

        for (Iterator<RuleEntry> iterator = ruleTree.iterator(); iterator.hasNext(); ) {
            RuleEntry entry = iterator.next();

            printRuleEntry(entry, builder, "", !iterator.hasNext());
        }

        return builder.toString();
    }

    @Data
    @Builder
    static class RuleEntry {

        private String name;

        private boolean used;

        private boolean defaultRule;

        private boolean actionTracing;

        @Singular
        private List<RuleEntry> children;
    }
}
