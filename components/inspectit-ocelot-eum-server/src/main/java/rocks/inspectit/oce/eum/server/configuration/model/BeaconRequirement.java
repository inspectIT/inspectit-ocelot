package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * Beacon requirements. Used for excluding beacons under certain circumstances.
 */
@Data
@Slf4j
public class BeaconRequirement {

    /**
     * Checks whether the given beacon fulfills all of the given requirement. In case the list of requirements is
     * empty or null the result will be `true`.
     *
     * @param beacon       the beacon to validate
     * @param requirements list of requirements which have to be fulfilled
     *
     * @return true in case the beacon fulfills the requirements otherwise false is returned
     */
    public static boolean validate(Beacon beacon, Collection<BeaconRequirement> requirements) {
        if (CollectionUtils.isEmpty(requirements)) {
            return true;
        }

        boolean notFulfilled = requirements.stream().anyMatch(requirement -> !requirement.validate(beacon));

        return !notFulfilled;
    }

    /**
     * The supported types of requirements.
     */
    public enum RequirementType {

        /**
         * The target field must exist.
         */
        EXISTS,

        /**
         * The target field must not exist.
         */
        NOT_EXISTS}

    /**
     * The field which is targeted.
     */
    @NotEmpty
    private String field;

    /**
     * the type of requirement.
     */
    @NotNull
    private RequirementType requirement;

    /**
     * Checks whether the given beacon fulfills this requirement.
     *
     * @param beacon the beacon to check
     *
     * @return true in case the beacon fulfills the requirement otherwise false is returned
     */
    public boolean validate(Beacon beacon) {
        switch (requirement) {
            case EXISTS:
                return beacon.contains(field);
            case NOT_EXISTS:
                return !beacon.contains(field);
            default:
                log.error("Requirement of type {} is not supported.", requirement);
                return false;
        }
    }
}
