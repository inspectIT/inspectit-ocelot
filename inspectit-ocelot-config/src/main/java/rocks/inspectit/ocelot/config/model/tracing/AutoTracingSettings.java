package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Defines global settings regarding the stack trace sampling feature.
 */
@Data
@NoArgsConstructor
public class AutoTracingSettings {

    /**
     * Defines the frequency at which stack trace samples are taken.
     * The higher the frequency, the greater the accuracy of the resulting trace.
     * <p>
     * However, taking stack traces is very expensive, therefore high frequencies can induce a big performance penalty.
     */
    private Duration frequency;

    /**
     * When the first method executes with auto-tracing enabled, a separate Thread is started as a timer for taking stack trace samples.
     * If at some point no more methods with stack-trace sampling are executed, this thread spins unnecessarily.
     * <p>
     * Therefore if no samples has been taken for at least the duration of {@link #shutdownDelay}, the timer will be shutdown.
     * If a span requests sampling after the timer has shutdown, it will restart it.
     */
    private Duration shutdownDelay;
}
