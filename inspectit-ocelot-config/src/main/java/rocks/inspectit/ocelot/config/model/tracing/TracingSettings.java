package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class TracingSettings {

    /**
     * Enum that defines when are common tags added to span attributes.
     */
    public enum AddCommonTags {
        NEVER,
        ON_GLOBAL_ROOT,
        ON_LOCAL_ROOT,
        ALWAYS
    }

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
    @Max(1)
    @Min(0)
    private double sampleProbability;

    /**
     * Settings for log correlation.
     */
    @Valid
    private LogCorrelationSettings logCorrelation = new LogCorrelationSettings();

    /**
     * Generically defines behavior of adding common tags to spans.
     */
    @NotNull
    private AddCommonTags addCommonTags;

    /**
     * Settings for automatic tracing (stack trace sampling)
     */
    @Valid
    private AutoTracingSettings autoTracing;

    /**
     * The propagation format to use.
     */
    @NotNull
    private PropagationFormat propagationFormat = PropagationFormat.B3;
}
