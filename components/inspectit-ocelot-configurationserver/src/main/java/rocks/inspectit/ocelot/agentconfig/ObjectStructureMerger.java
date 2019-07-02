package rocks.inspectit.ocelot.agentconfig;


import java.util.*;
import java.util.stream.Stream;

/**
 * Merges structures of nested Maps and Lists in the same way spring would
 * merge them as property sources.
 */
public class ObjectStructureMerger {

    public static Object merge(Object first, Object second) {
        if (first instanceof Map && second instanceof Map) {
            return mergeMaps((Map<?, ?>) first, (Map<?, ?>) second);
        } else if (first instanceof List && second instanceof List) {
            return mergeLists((List<?>) first, (List<?>) second);
        } else {
            return first;
        }
    }

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

}
