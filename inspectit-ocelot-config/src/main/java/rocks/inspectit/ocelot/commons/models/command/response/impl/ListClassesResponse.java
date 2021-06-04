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

    /**
     * Existing types (classes and interfaces) including their methods.
     */
    private TypeElement[] result;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeElement {

        /**
         * The class or interface name.
         */
        private String name;

        /**
         * Whether it is a class or interace.
         */
        private String type;

        /**
         * Signatures of available methods.
         */
        private String[] methods;
    }
}

