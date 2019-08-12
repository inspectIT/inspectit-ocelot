package rocks.inspectit.oce.eum.server.beacon;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconRequirement;

import java.util.*;

@Slf4j
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

    private boolean checkRequirement(BeaconRequirement requirement) {
        switch (requirement.getRequirement()) {
            case NOT_EXISTS:
                return !contains(requirement.getField());
            default:
                log.error("Requirement of type {} is not supported.", requirement.getRequirement());
                return false;
        }
    }

    public boolean checkRequirements(Collection<BeaconRequirement> requirements) {
        if (requirements == null) {
            return true;
        }

        boolean notFulfilled = requirements.stream()
                .anyMatch(requirement -> !checkRequirement(requirement));

        return !notFulfilled;
    }
}
