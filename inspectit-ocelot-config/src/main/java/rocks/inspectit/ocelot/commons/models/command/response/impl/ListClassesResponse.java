package rocks.inspectit.ocelot.commons.models.command.response.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ListClassesResponse extends CommandResponse {

    private TypeElement[] result;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeElement {

        private String name;

        private String type;

        private String[] methods;
    }
}

