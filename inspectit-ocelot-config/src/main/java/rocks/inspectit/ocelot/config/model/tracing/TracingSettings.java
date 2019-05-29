package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;

@Data
@NoArgsConstructor
public class TracingSettings {

    /**
     * Master switch for disabling trace recording and exporting.
     * If disabled the following happens:
     * - all trace exporters are disabled
     * - tracing will be disabled for all instrumentation rules
     */
    private boolean enabled;

    /**
     * The default sample probability to use for {@link rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings#sampleProbability},
     * in case no value is specified in the individual rules.
     */
    private double sampleProbability;

    @AssertTrue(message = "sampleProbability must be in the range [0,1]")
    public boolean isSampleProbabilityInRange() {
        return sampleProbability >= 0 && sampleProbability <= 1;
    }
}
