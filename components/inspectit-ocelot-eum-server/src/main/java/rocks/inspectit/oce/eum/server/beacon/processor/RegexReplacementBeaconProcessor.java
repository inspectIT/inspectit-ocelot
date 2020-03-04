package rocks.inspectit.oce.eum.server.beacon.processor;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;

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
        Map<String, RegexDerivedTag> unorderedTags = config.getTags().getRegex().entrySet().stream()
                .map(e ->
                        RegexDerivedTag.builder()
                                .tagName(e.getKey())
                                .inputBeaconField(e.getValue().getInput())
                                .regex(Pattern.compile(e.getValue().getRegex()))
                                .replacement(e.getValue().getReplacement())
                                .keepIfNoMatch(e.getValue().isKeepOriginalIfNoMatch())
                                .build()
                )
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
        for (RegexDerivedTag derived : derivedTags) {
            String input = newTags.get(derived.getInputBeaconField());
            if (input == null) {
                input = beacon.get(derived.getInputBeaconField());
            }
            if (input != null) {
                if (derived.getRegex().matcher(input).find()) {
                    newTags.put(derived.getTagName(), derived.getRegex().matcher(input).replaceAll(derived.getReplacement()));
                } else if (derived.isKeepIfNoMatch()) {
                    newTags.put(derived.getTagName(), input);
                }
            }
        }
        return beacon.merge(newTags);
    }

    /**
     * Returns the given elements as a sorted list,
     * ensuring that each element appears after it's dependencies.
     *
     * @param elements        the elements to sort
     * @param getDependencies a function given an element returning its dependencies
     * @param <T>             the element types, should have proper hashcode /equals implementation
     * @return the sorted list of all elements
     */
    public <T> List<T> getInTopologicalOrder(Collection<T> elements, Function<T, Collection<T>> getDependencies) {
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
            if (!result.contains(dependency)) {
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
         * The regular expression to use for the "replace-all" operation.
         */
        Pattern regex;

        /**
         * The replacement pattern to use for each match.
         */
        String replacement;

        /**
         * If true, the original input value will be kept if no regex match is found.
         * Otherwise no value for {@link #tagName} will be set in the beacon.
         */
        boolean keepIfNoMatch;
    }
}
