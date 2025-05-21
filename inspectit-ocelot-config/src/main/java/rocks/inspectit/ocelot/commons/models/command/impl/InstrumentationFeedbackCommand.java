package rocks.inspectit.ocelot.commons.models.command.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.CommandResponse;

import java.util.List;
import java.util.Map;

/**
 * Command for requesting the currently applied instrumentation.
 * The command response contains a set of classes, theirs methods and the active rules for these methods.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstrumentationFeedbackCommand extends Command {

    /**
     * Type identifier for JSON serialization.
     */
    public static final String TYPE_IDENTIFIER = "instrumentation-feedback";

    /**
     * Contains the set of classes and their instrumentation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends CommandResponse {
        private Map<String, ClassInstrumentation> instrumentationFeedback;
    }

    /**
     * Contains the set of methods and their instrumentation rules for a specific class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInstrumentation {
        private Map<String, List<String>> classInstrumentation;
    }
}
