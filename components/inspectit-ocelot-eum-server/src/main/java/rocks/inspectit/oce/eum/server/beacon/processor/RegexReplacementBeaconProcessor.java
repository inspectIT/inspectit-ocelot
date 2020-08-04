package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconTagSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.configuration.model.PatternAndReplacement;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Performs regex replacements on beacon fields.
 * Every replacement results in a new beacon field.
 */
@Slf4j
@Component
public class RegexReplacementBeaconProcessor implements BeaconProcessor {

    private List<RegexDerivedTag> derivedTags;

    @Autowired
    public RegexReplacementBeaconProcessor(EumServerConfiguration config) {
        Map<String, RegexDerivedTag> unorderedTags = config.getTags().getBeacon().entrySet().stream()
                .map(e -> RegexDerivedTag.fromSettings(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(RegexDerivedTag::getTagName, t -> t));
        derivedTags = getInTopologicalOrder(unorderedTags.values(), tag -> {
            String input = tag.getInputBeaconField();
            if (unorderedTags.containsKey(input)) {
                return Collections.singletonList(unorderedTags.get(input));
            } else {
                return Collections.emptyList();
            }
        });
    }

    @Override
    public Beacon process(Beacon beacon) {
        Map<String, String> newTags = new HashMap<>();

        for (RegexDerivedTag derivedTag : derivedTags) {
            String input = newTags.get(derivedTag.getInputBeaconField());
            if (input == null) {
                input = beacon.get(derivedTag.getInputBeaconField());
            }

            String tagValue = deriveTag(derivedTag, input);
            if (tagValue != null) {
                newTags.put(derivedTag.getTagName(), tagValue);
            }
        }

        return beacon.merge(newTags);
    }

    private String deriveTag(RegexDerivedTag tag, String input) {
        String value = input;
        if (value == null) {
            if (tag.isNullAsEmpty()) {
                value = "";
            } else {
                return null;
            }
        }
        return applyAllReplacements(value, tag.getReplacements());
    }

    private String applyAllReplacements(String input, List<PatternAndReplacement> replacements) {
        String result = input;
        for (PatternAndReplacement setting : replacements) {
            if (result == null) {
                return null;
            }
            result = applyReplacement(setting, result);
        }
        return result;
    }

    private String applyReplacement(PatternAndReplacement replacement, String value) {
        Pattern regex = Pattern.compile(replacement.getPattern());
        try {
            boolean anyRegexMatch = regex.matcher(value).find();
            if (anyRegexMatch) {
                return regex.matcher(value).replaceAll(replacement.getReplacement());
            }
        } catch (Exception ex) {
            log.error("Error applying replacement regex <{}> with replacement <{}>!", replacement.getPattern(), replacement
                    .getReplacement());
        }
        if (replacement.isKeepNoMatch()) {
            return value;
        } else {
            return null;
        }
    }

    /**
     * Returns the given elements as a sorted list,
     * ensuring that each element appears after it's dependencies.
     *
     * @param elements        the elements to sort
     * @param getDependencies a function given an element returning its dependencies
     * @param <T>             the element types, should have proper hashcode /equals implementation
     *
     * @return the sorted list of all elements
     */
    private <T> List<T> getInTopologicalOrder(Collection<T> elements, Function<T, Collection<T>> getDependencies) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        LinkedHashSet<T> visited = new LinkedHashSet<>();

        elements.forEach(e -> addInTopologicalOrder(e, getDependencies, visited, result));

        return new ArrayList<>(result);
    }

    private <T> void addInTopologicalOrder(T current, Function<T, Collection<T>> getDependencies, LinkedHashSet<T> currentPath, LinkedHashSet<T> result) {
        if (currentPath.contains(current)) {
            throw new RuntimeException("Cyclic dependency detected!");
        }
        currentPath.add(current);
        for (T dependency : getDependencies.apply(current)) {
            if (!result.contains(dependency) && !dependency.equals(current)) {
                addInTopologicalOrder(dependency, getDependencies, currentPath, result);
            }
        }
        result.add(current);
        currentPath.remove(current);
    }

    @Value
    @Builder
    private static class RegexDerivedTag {

        /**
         * The name of the resulting tag / beacon field under which the result is stored
         */
        String tagName;

        /**
         * The input beacon field.
         */
        String inputBeaconField;

        /**
         * Specify whether the input field should be considered as an empty string if it does not exists.
         */
        boolean nullAsEmpty;

        List<PatternAndReplacement> replacements;

        private static RegexDerivedTag fromSettings(String tagName, BeaconTagSettings settings) {
            return RegexDerivedTag.builder()
                    .tagName(tagName)
                    .inputBeaconField(settings.getInput())
                    .nullAsEmpty(settings.isNullAsEmpty())
                    .replacements(settings.getAllReplacements())
                    .build();
        }
    }
}
