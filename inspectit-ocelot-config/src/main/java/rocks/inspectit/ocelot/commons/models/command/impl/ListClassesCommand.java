package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

/**
 * Command for requesting a list of available classes and methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ListClassesCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "list-classes";

    /**
     * Filter query to filter the resulting class set.
     */
    private String filter;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {

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
             * Whether it is a class or interface.
             */
            private String type;

            /**
             * Signatures of available methods.
             */
            private String[] methods;
        }
    }
}
