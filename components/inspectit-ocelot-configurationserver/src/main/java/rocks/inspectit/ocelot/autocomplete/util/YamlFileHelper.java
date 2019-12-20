package rocks.inspectit.ocelot.autocomplete.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class YamlFileHelper {

    @Autowired
    YamlLoader yamlLoader;

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
     * Extracts all keys from a given map that can be found in a given path
     *
     * @param o       the map one wants to extract the keys from
     * @param mapPath all keys leading to the wanted
     * @return
     */
    private List<String> extractKeys(Object o, List<String> mapPath) {
        if (o instanceof List) {
            if (mapPath.size() == 0) {
                return (List) o;
            }
            return extractKeysFromList((List) o, mapPath);
        }
        if (o instanceof Map) {
            if (mapPath.size() == 0) {
                return new ArrayList<>(((Map) o).keySet());
            }
            return extractKeysFromMap((Map) o, mapPath);
        }
        if (o instanceof String) {
            return Arrays.asList((String) o);
        }
        return new ArrayList<>();
    }

    private List<String> extractKeysFromList(List list, List<String> mapPath) {
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
            return extractKeys(list.get(i), mapPath);
        }

    }

    private List<String> extractKeysFromMap(Map map, List<String> mapPath) {
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
