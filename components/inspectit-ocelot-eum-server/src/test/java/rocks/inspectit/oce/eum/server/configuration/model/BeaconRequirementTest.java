package rocks.inspectit.oce.eum.server.configuration.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class BeaconRequirementTest {

    @Nested
    public class StaticValidate {

        private Beacon beacon;

        @BeforeEach
        public void before() {
            HashMap<String, String> map = new HashMap<>();
            map.put("first", "1");
            map.put("second", "2");
            beacon = Beacon.of(map);
        }

        @Test
        public void noFulfillment() {
            BeaconRequirement requirementA = new BeaconRequirement();
            requirementA.setField("third");
            requirementA.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);
            BeaconRequirement requirementB = new BeaconRequirement();
            requirementB.setField("second");
            requirementB.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);

            boolean result = BeaconRequirement.validate(beacon, Arrays.asList(requirementA, requirementB));

            assertThat(result).isFalse();
        }

        @Test
        public void fulfillment() {
            BeaconRequirement requirementA = new BeaconRequirement();
            requirementA.setField("third");
            requirementA.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);
            BeaconRequirement requirementB = new BeaconRequirement();
            requirementB.setField("second");
            requirementB.setRequirement(BeaconRequirement.RequirementType.EXISTS);

            boolean result = BeaconRequirement.validate(beacon, Arrays.asList(requirementA, requirementB));

            assertThat(result).isTrue();
        }

        @Test
        public void nullList() {
            boolean result = BeaconRequirement.validate(beacon, null);

            assertThat(result).isTrue();
        }

        @Test
        public void emptyList() {
            boolean result = BeaconRequirement.validate(beacon, Collections.emptyList());

            assertThat(result).isTrue();
        }
    }

    @Nested
    public class Validate {

        private Beacon beacon = Beacon.of(Collections.singletonMap("field", "5"));

        private BeaconRequirement requirement = new BeaconRequirement();

        @Test
        public void noFulfillment_notExists() {
            requirement.setField("field");
            requirement.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);

            boolean result = requirement.validate(beacon);

            assertThat(result).isFalse();
        }

        @Test
        public void fulfillment_notExists() {
            requirement.setField("another");
            requirement.setRequirement(BeaconRequirement.RequirementType.NOT_EXISTS);

            boolean result = requirement.validate(beacon);

            assertThat(result).isTrue();
        }

        @Test
        public void noFulfillment_exists() {
            requirement.setField("another");
            requirement.setRequirement(BeaconRequirement.RequirementType.EXISTS);

            boolean result = requirement.validate(beacon);

            assertThat(result).isFalse();
        }

        @Test
        public void fulfillment_exists() {
            requirement.setField("field");
            requirement.setRequirement(BeaconRequirement.RequirementType.EXISTS);

            boolean result = requirement.validate(beacon);

            assertThat(result).isTrue();
        }
    }
}