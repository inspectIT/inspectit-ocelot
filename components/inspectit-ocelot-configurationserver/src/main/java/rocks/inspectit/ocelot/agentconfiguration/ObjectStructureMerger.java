package rocks.inspectit.ocelot.agentconfiguration;

import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.util.*;
import java.util.stream.Stream;

/**
 * Merges structures of nested Maps and Lists in the same way spring would
 * merge them as property sources.
 */
public class ObjectStructureMerger {

    /**
     * Loads a yaml file as a Map/List structure and merges it with an existing map/list structure.
     *
     * @param src the source yaml, which should be merged
     * @param toMerge the existing structure of nested maps / lists with which the loaded yaml will be merged.
     * @param path    the path of the yaml file to load
     *
     * @return the merged structure
     */
    public static Object loadAndMergeYaml(String src, Object toMerge, String path) {
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> loadedYaml = yaml.load(src);
            if (toMerge == null) {
                return loadedYaml;
            } else {
                return ObjectStructureMerger.merge(toMerge, loadedYaml);
            }
        } catch (Exception e) {
            throw new InvalidConfigurationFileException(path, e);
        }
    }

    /**
     * Tries to merge the given two Object structure, giving precedence to the first one.
     * If both Objects are maps or bot hare lists, they will be deeply merged.
     * Otherwise, simply "first" is returned.
     *
     * @param first  the object structure to take precedence
     * @param second the second object structure
     * @return
     */
    public static Object merge(Object first, Object second) {
        if (first instanceof Map && second instanceof Map) {
            return mergeMaps((Map<?, ?>) first, (Map<?, ?>) second);
        } else if (first instanceof List && second instanceof List) {
            return mergeLists((List<?>) first, (List<?>) second);
        } else {
            return first;
        }
    }

    /**
     * Merges two non-null maps deeply.
     * This means if a given key exists in both maps, {@link #merge(Object, Object)} will be called for its value.
     *
     * @param first  the first map to merge
     * @param second the second map to merge
     * @return the merged map
     */
    private static Map<?, ?> mergeMaps(Map<?, ?> first, Map<?, ?> second) {
        //use a linked hashmap to potentially preserve the order
        Map<Object, Object> result = new LinkedHashMap<>();
        Stream.concat(first.keySet().stream(), second.keySet().stream())
                .distinct()
                .forEach(key -> {
                    boolean firstContains = first.containsKey(key);
                    boolean secondContains = second.containsKey(key);
                    if (firstContains && secondContains) {
                        result.put(key, merge(first.get(key), second.get(key)));
                    } else if (firstContains) {
                        result.put(key, first.get(key));
                    } else if (secondContains) {
                        result.put(key, second.get(key));
                    }
                });
        return result;
    }

    /**
     * Merges two non-null lists deeply.
     * This means if a element at a given index exists in both lists, {@link #merge(Object, Object)} will be called for its value.
     *
     * @param first  the first list to merge
     * @param second the second list to merge
     * @return the merged map
     */
    private static List<?> mergeLists(List<?> first, List<?> second) {
        List<Object> result = new ArrayList<>();

        Iterator<?> firstIt = first.iterator();
        Iterator<?> secondIt = second.iterator();

        while (firstIt.hasNext() && secondIt.hasNext()) {
            result.add(merge(firstIt.next(), secondIt.next()));
        }
        while (firstIt.hasNext()) {
            result.add(firstIt.next());
        }
        while (secondIt.hasNext()) {
            result.add(secondIt.next());
        }
        return result;
    }

    /**
     * This exception will be thrown if a configuration file cannot be parsed, e.g. it contains invalid characters.
     */
    static class InvalidConfigurationFileException extends RuntimeException {

        public InvalidConfigurationFileException(String path, Exception e) {
            super(String.format("The configuration file '%s' is invalid and cannot be parsed.", path), e);
        }
    }
}
