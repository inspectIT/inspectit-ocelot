package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class BeaconRequirement {

    public enum RequirementType {
        NOT_EXISTS
    }

    @NotEmpty
    private String field;

    @NotNull
    private RequirementType requirement;
}
