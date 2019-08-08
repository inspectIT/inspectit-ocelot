package rocks.inspectit.oce.eum.server.beacon;

import java.util.*;

public class Beacon {

    public static Beacon of(Map<String, String> beaconMap) {
        HashMap<String, String> map = new HashMap<>(beaconMap);
        return new Beacon(map);
    }

    private Map<String, String> map;

    private Beacon(Map<String, String> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    public boolean contains(String... fieldKeys) {
        return contains(Arrays.asList(fieldKeys));
    }

    public boolean contains(Collection<String> fieldKeys) {
        boolean fieldMissing = fieldKeys.stream()
                .anyMatch(field -> !map.containsKey(field));

        return !fieldMissing;
    }

    public String get(String fieldKey) {
        return map.get(fieldKey);
    }
}
