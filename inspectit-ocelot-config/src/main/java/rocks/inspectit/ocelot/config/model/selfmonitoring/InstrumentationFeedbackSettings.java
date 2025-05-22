package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InstrumentationFeedbackSettings {

    /**
     * Whether commands are enabled or not
     */
    private boolean enabled = false;

    /**
     * Include instrumented methods
     */
    private boolean includeMethods;


    /**
     * Include the particular rules, which cause the instrumentation
     */
    private boolean includeRules;
}
