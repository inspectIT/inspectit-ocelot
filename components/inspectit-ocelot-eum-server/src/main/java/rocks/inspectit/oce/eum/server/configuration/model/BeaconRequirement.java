package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;

@Data
public class BeaconRequirement {

    public enum RequirementType {
        NOT_EXISTS
    }

    private String field;

    private RequirementType requirement;
}
