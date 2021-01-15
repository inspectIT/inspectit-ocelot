package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

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
        return requirements.stream().allMatch(requirement -> requirement.validate(beacon));
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
        NOT_EXISTS,

        /**
         * Checks if the beacon has an initiator type listed in the {@link #initiators} array.
         */
        HAS_INITIATOR,
    }

    /**
     * the type of requirement.
     */
    @NotNull
    private RequirementType requirement;

    /**
     * The list of allowed initiators for the {@link RequirementType#HAS_INITIATOR} requirement.
     */
    private List<InitiatorType> initiators;

    /**
     * The field which is targeted.
     * Used for the {@link RequirementType#EXISTS} and {@link RequirementType#NOT_EXISTS} requirement types.
     */
    private String field;

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
            case HAS_INITIATOR:
                return initiators.stream().anyMatch(initiatorType -> initiatorType.hasInitiator(beacon));
            default:
                log.error("Requirement of type {} is not supported.", requirement);
                return false;
        }
    }

    @AssertTrue
    public boolean isValid() {
        switch (requirement) {
            case EXISTS:
            case NOT_EXISTS:
                return !StringUtils.isEmpty(field);
            case HAS_INITIATOR:
                return !CollectionUtils.isEmpty(initiators);
            default:
                log.error("Requirement of type {} is not supported.", requirement);
                return false;
        }
    }
}
