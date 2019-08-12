package rocks.inspectit.oce.eum.server.beacon;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconRequirement;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BeaconTest {

    @Nested
    public class CheckRequirements {

        @Test
        public void t() {
            Beacon beacon = Beacon.of(Collections.singletonMap("field", "5"));
            BeaconRequirement requirement = new BeaconRequirement();
            requirement.setField("field");
            requirement.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);

            boolean result = beacon.checkRequirements(Collections.singletonList(requirement));

            assertThat(result).isFalse();
        }

        @Test
        public void t2() {
            Beacon beacon = Beacon.of(Collections.singletonMap("field", "5"));
            BeaconRequirement requirement = new BeaconRequirement();
            requirement.setField("another");
            requirement.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);

            boolean result = beacon.checkRequirements(Collections.singletonList(requirement));

            assertThat(result).isTrue();
        }

    }

}