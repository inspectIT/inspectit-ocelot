package rocks.inspectit.ocelot.autocomplete.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConfigurationQueryHelper {

    @Autowired
    ConfigurationFilesCache configurationFilesCache;

    /**
     * This method takes a list that resembles a path and searches this path in all .yml or .yaml files in the servers
     * directory.
     * The returned list contains all elements to which the path could be extended.
     * e.g.: The file a.yaml contains the path inspectit: path: to: my: attribute.
     * The list {"inspectit","path"} is given as attribute to this method.
     * The method returns a list containing "to".
     *
     * @param path A path parsed to a List.
     * @return The attributes which could be found in the given path.
     */
    public List<String> getKeysForPath(List<String> path) {
        return configurationFilesCache.getParsedContents()
                .stream()
                .flatMap(root -> extractKeys(root, path).stream())
                .collect(Collectors.toList());
    }

    /**
     * Extracts all keys from a given object that can be found at the end of a given path.
     * If the path is not empty, this method iterates further through the given path.
     * If the path is empty, the current objects attributes is returned:
     * In case this current object is a List, the List itself is returned.
     * In case this current object is a Map, the keyset of the map is returned.
     * In case this current object is a String, a List containing only this String is returned.
     *
     * @param o       The object the keys should be returned from.
     * @param mapPath The path leading to the keys that should be retrieved.
     * @return
     */
    private List<String> extractKeys(Object o, List<String> mapPath) {
        if (o instanceof List) {
            return extractKeysFromList((List) o, mapPath);
        }
        if (o instanceof Map) {
            return extractKeysFromMap((Map) o, mapPath);
        }
        if (o instanceof String) {
            if (mapPath.size() == 0) {
                return Collections.singletonList((String) o);
            }
            return Collections.emptyList();
        }
        return new ArrayList<>();
    }

    /**
     * Takes a Collection and adds each object within it that is an instance of String to a List.
     * Then the list is returned.
     *
     * @param collection The collection which should be parsed.
     * @return A List containing all Strings found in the given Collection.
     */
    private List<String> toStringList(Collection<?> collection) {
        return collection.stream()
                .filter(content -> content instanceof String)
                .map(content -> (String) content)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list's contents if the mapPath only contains one element.
     * if "mapPath" = {"*"} -> all list values are returned.
     * if "mapPath"= {*NUMBER*} -> the list element at the given index is returned.
     * if "mapPath" is longer than one element, the search descends down into one or all list elements respectively.
     *
     * @param list    The list which contents should be searched.
     * @param mapPath The path which should be checked.
     * @return a list of strings containing the attributes that could found in the mapPath.
     */
    private List<String> extractKeysFromList(List<?> list, List<String> mapPath) {
        if (mapPath.size() == 0) {
            return toStringList(list);
        }
        String currentLiteral = mapPath.get(0);
        List<String> subPath = mapPath.subList(1, mapPath.size());
        if (currentLiteral.equals("*")) {
            ArrayList<String> toReturn = new ArrayList<>();
            for (Object key : list) {
                toReturn.addAll(extractKeys(key, subPath));
            }
            return toReturn;
        } else {
            int i = 0;
            try {
                i = Integer.parseInt(currentLiteral);
            } catch (NumberFormatException e) {
                log.error("Invalid List-Index {}", currentLiteral);
                return Collections.emptyList();
            }
            if (i >= 0 && i < list.size()) {
                return extractKeys(list.get(i), subPath);
            }
            return Collections.emptyList();
        }

    }

    /**
     * Returns the map's keys if the mapPath only contains one element.
     * if "mapPath" = {"*"} -> all list values are returned.
     * if "mapPath" contains only one element, the value which is referenced by this key is returned.
     * if "mapPath" is longer than one element, the search descends down into one or all list elements respectively.
     *
     * @param map     The list which contents should be searched.
     * @param mapPath The path which should be checked.
     * @return a list of strings containing the attributes that could found in the mapPath.
     */
    private List<String> extractKeysFromMap(Map<?, ?> map, List<String> mapPath) {
        if (mapPath.size() == 0) {
            return toStringList(((Map) map).keySet());
        }
        String currentLiteral = mapPath.get(0);
        List<String> subPath = mapPath.subList(1, mapPath.size());
        if (currentLiteral.equals("*")) {
            return map.values().stream()
                    .flatMap(value -> extractKeys(value, subPath).stream())
                    .collect(Collectors.toList());
        } else {
            return extractKeys(map.get(currentLiteral), subPath);
        }
    }
}
