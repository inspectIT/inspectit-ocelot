package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

/**
 * Command for requesting the currently applied instrumentation.
 * The command response contains a set of classes, theirs methods and the particular rules, which caused
 * the instrumentation.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstrumentationFeedbackCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "instrumentation-feedback";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {

        /**
         * JSON string with the currently applied instrumentation
         */
        private String instrumentationFeedback;
    }
}
