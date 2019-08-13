package rocks.inspectit.oce.eum.server.beacon;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Container for beacons send by the EUM agent.
 */
@Slf4j
public class Beacon {

    /**
     * Creates a {@link Beacon} instance based on the given map.
     *
     * @param beaconMap map which is used as base for the created {@link Beacon}
     * @return a new {@link Beacon} instance
     */
    public static Beacon of(Map<String, String> beaconMap) {
        HashMap<String, String> map = new HashMap<>(beaconMap);
        return new Beacon(map);
    }

    /**
     * The beacon's property map.
     */
    private Map<String, String> map;

    /**
     * Constructor.
     */
    private Beacon(Map<String, String> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    /**
     * Checks whether the beacon contains all of the given fields.
     *
     * @param fieldKeys field keys to check
     * @return true in case all specified field keys are existing otherwise false
     */
    public boolean contains(String... fieldKeys) {
        return contains(Arrays.asList(fieldKeys));
    }

    /**
     * Checks whether the beacon contains all of the fields contained in the given list.
     *
     * @param fieldKeys List containing field keys to check
     * @return true in case all specified field keys are existing otherwise false
     */
    public boolean contains(Collection<String> fieldKeys) {
        boolean fieldMissing = fieldKeys.stream()
                .anyMatch(field -> !map.containsKey(field));

        return !fieldMissing;
    }

    /**
     * Returns the value associated with the given key. `null` will be returned in case the key does not exist.
     *
     * @param fieldKey the field key of the field to get
     * @return the value of the specified key
     */
    public String get(String fieldKey) {
        return map.get(fieldKey);
    }
}
