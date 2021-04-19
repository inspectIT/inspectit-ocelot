package rocks.inspectit.oce.eum.server.beacon;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Container for beacons send by the EUM agent.
 */
@Slf4j
@JsonSerialize(using = BeaconECSSerializer.class)
public class Beacon {

    /**
     * Creates a {@link Beacon} instance based on the given map.
     *
     * @param beaconMap map which is used as base for the created {@link Beacon}
     *
     * @return a new {@link Beacon} instance
     */
    public static Beacon of(Map<String, String> beaconMap) {
        HashMap<String, String> map = new HashMap<>(beaconMap);
        return new Beacon(map);
    }

    /**
     * Merges two {@link Beacon}s. Existing values of beacon1 will be overwritten by values of beacon2.
     *
     * @param beacon1 The first Beacon
     * @param beacon2 The second Beacon
     *
     * @return A new Beacon instance.
     */
    public static Beacon merge(Beacon beacon1, Beacon beacon2) {
        return Beacon.of(Stream.of(beacon1.map, beacon2.map).collect(HashMap::new, Map::putAll, Map::putAll));
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
     * Merges this beacon with the given map.
     *
     * @param beaconMap The map to be added as beacon properties
     *
     * @return A new {@link Beacon} instance
     */
    public Beacon merge(Map<String, String> beaconMap) {
        return merge(Beacon.of(beaconMap));
    }

    /**
     * Merges this beacon with the given one.
     *
     * @param beacon The {@link Beacon} to be merged with this one.
     *
     * @return A new {@link Beacon} instance
     */
    public Beacon merge(Beacon beacon) {
        return Beacon.merge(this, beacon);
    }

    /**
     * Checks whether the beacon contains all of the given fields.
     *
     * @param fieldKeys field keys to check
     *
     * @return true in case all specified field keys are existing otherwise false
     */
    public boolean contains(String... fieldKeys) {
        return contains(Arrays.asList(fieldKeys));
    }

    /**
     * Checks whether the beacon contains all of the fields contained in the given list.
     *
     * @param fieldKeys List containing field keys to check
     *
     * @return true in case all specified field keys are existing otherwise false
     */
    public boolean contains(Collection<String> fieldKeys) {
        boolean fieldMissing = fieldKeys.stream().anyMatch(field -> !map.containsKey(field));

        return !fieldMissing;
    }

    /**
     * Returns the value associated with the given key. `null` will be returned in case the key does not exist.
     *
     * @param fieldKey the field key of the field to get
     *
     * @return the value of the specified key
     */
    public String get(String fieldKey) {
        return map.get(fieldKey);
    }

    /**
     * Returns the content of this beacon as map.
     *
     * @return a new Map representing the beacon
     */
    public Map<String, String> toMap() {
        return map;
    }
}
