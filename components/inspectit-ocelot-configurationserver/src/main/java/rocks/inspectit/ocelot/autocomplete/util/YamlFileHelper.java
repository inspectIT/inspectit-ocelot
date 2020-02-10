package rocks.inspectit.ocelot.autocomplete.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YamlFileHelper {

    @Autowired
    YamlLoader yamlLoader;

    /**
     * This Method takes a path parsed to a List and searches this path in all .yml or .yaml files in the servers
     * directory.
     * The returned list contains all elements to which the path could be extended.
     * e.g.: The file a.yaml contains the path inspectit.path.to.my.attribute.
     * The list {"inspectit","path"} is given as attribute to this method.
     * The method returns the list {"to"}
     *
     * @param path A path parsed to a List.
     * @return The attributes which could be found in the given path.
     */
    public List<String> extractKeysFromYamlFiles(List<String> path) {
        ArrayList<String> toReturn = new ArrayList<>();
        Collection objects = yamlLoader.getYamlContents();
        for (Object o : objects) {
            List<String> extractedKeys = extractKeys(o, new ArrayList<>(path));
            if (extractedKeys != null) {
                toReturn.addAll(extractedKeys);
            }
        }
        return toReturn;
    }

    /**
     * Extracts all keys from a given object that can be found at the end of a given path.
     * If the path is not empty, this method iterates further through the given path.
     * If the path is empty, the current objects attributes is returned.
     * In case the current object is a List, the List itself is returned.
     * In case the current object is a Map, the keyset of the map is returned.
     * In case the current object is a String, a List containing only this String is returned.
     *
     * @param o       The object the keys should be returned from.
     * @param mapPath The path leading to the keys that should be retrieved.
     * @return
     */
    private List<String> extractKeys(Object o, List<String> mapPath) {
        if (o instanceof List) {
            if (mapPath.size() == 0) {
                return toStringList((List) o);
            }
            return extractKeysFromList((List) o, mapPath);
        }
        if (o instanceof Map) {
            if (mapPath.size() == 0) {
                return toStringList(((Map) o).keySet());
            }
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

    private List<String> toStringList(Collection<?> collection) {
        return collection.stream()
                .filter(content -> content instanceof String)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list's contents if the mapPath only contains one element.
     * Iterates further through all elements of the list if mapPath contains more than one element.
     *
     * @param list    The list which contents should be searched.
     * @param mapPath The path which should be checked.
     * @return a list of strings containing the attributes that could found in the mapPath.
     */
    private List<String> extractKeysFromList(List<?> list, List<String> mapPath) {
        String currentLiteral = mapPath.remove(0);
        if (currentLiteral.equals("*")) {
            ArrayList<String> toReturn = new ArrayList<>();
            for (Object key : list) {
                toReturn.addAll(extractKeys(key, new ArrayList<>(mapPath)));
            }
            return toReturn;
        } else {
            int i = 0;
            try {
                i = Integer.parseInt(currentLiteral);
            } catch (NumberFormatException e) {
                log.error("Invalid List-Index {}", currentLiteral);
                return null;
            }
            if (i >= 0 && i < list.size()) {
                return extractKeys(list.get(i), mapPath);
            }
            return Collections.emptyList();
        }

    }

    /**
     * Returns the map's keys if the mapPath only contains one element.
     * Iterates further through all elements of the map if mapPath contains more than one element.
     *
     * @param map     The list which contents should be searched.
     * @param mapPath The path which should be checked.
     * @return a list of strings containing the attributes that could found in the mapPath.
     */
    private List<String> extractKeysFromMap(Map<?, ?> map, List<String> mapPath) {
        String currentLiteral = mapPath.remove(0);
        if (currentLiteral.equals("*")) {
            ArrayList<String> toReturn = new ArrayList<>();
            for (Object key : map.keySet()) {
                if (key instanceof String) {
                    toReturn.addAll(extractKeys(map.get(key), new ArrayList<>(mapPath)));
                }
            }
            return toReturn;
        } else {
            return extractKeys(map.get(currentLiteral), mapPath);
        }
    }
}
