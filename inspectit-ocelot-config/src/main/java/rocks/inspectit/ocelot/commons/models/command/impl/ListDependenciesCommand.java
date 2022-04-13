package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

/**
 * Command for requesting a list of used dependencies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ListDependenciesCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "list-dependencies";

    private String dependencyFormat = "TBD"; //determine a format that is optimal for easy copy and pasting of the results to a database for a check

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {

        /**
         * Existing dependencies including their version.
         */
        private DependecyElement[] result;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DependecyElement {

            /**
             * The dependency name.
             */
            private String name;

            /**
             * Whether it is a class or interface.
             */
            private String version;

        }
    }
}
