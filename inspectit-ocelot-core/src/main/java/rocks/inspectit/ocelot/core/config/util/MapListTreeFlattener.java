package rocks.inspectit.ocelot.core.config.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class flattens nested structures of Maps and Lists to a single Map from String to Object.
 * The keys in the result map follow the typical naming scheme used by spring for nested properties.
 */
public class MapListTreeFlattener {

    /**
     * Flattens the given List or Map
     *
     * @param root the root list or map
     *
     * @return the flattened map.
     */
    public static Map<String, Object> flatten(Object root) {
        Map<String, Object> results = new HashMap<>();
        flatten("", root, results);
        return results;
    }

    private static boolean isContainer(Object obj) {
        return obj instanceof List || obj instanceof Map;
    }

    private static void flatten(String prefix, Object node, Map<String, Object> results) {
        if (node instanceof Map) {
            flattenDispatch(prefix, (Map<?, ?>) node, results);
        } else if (node instanceof List) {
            flattenDispatch(prefix, (List<?>) node, results);
        } else {
            throw new RuntimeException("Unexpected type: " + node.getClass().getName());
        }
    }

    private static void flattenDispatch(String prefix, Map<?, ?> node, Map<String, Object> results) {
        node.forEach((k, v) -> {
            String key = k.toString();
            String name;
            if (prefix.isEmpty()) {
                name = key;
            } else {
                name = prefix + "." + key;
            }
            if (isContainer(v)) {
                flatten(name, v, results);
            } else {
                results.put(name, v);
            }
        });
    }

    private static void flattenDispatch(String prefix, List<?> node, Map<String, Object> results) {
        for (int i = 0; i < node.size(); i++) {
            String name = prefix + "[" + i + "]";
            Object v = node.get(i);
            if (isContainer(v)) {
                flatten(name, v, results);
            } else {
                results.put(name, v);
            }
        }
    }

}
