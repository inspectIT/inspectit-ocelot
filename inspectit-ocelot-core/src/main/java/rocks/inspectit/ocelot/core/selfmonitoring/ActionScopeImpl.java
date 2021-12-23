package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.concurrent.TimeUnit;

/**
 * Functional implementation of the {@link IActionScope}
 */
@Value
@Slf4j
public class ActionScopeImpl implements IActionScope {

    /**
     * The {@link IHookAction} of this scope.
     */
    private final IHookAction action;

    /**
     * The start time of the action/scope in nanoseconds.
     */
    private final long startTimeNanos;

    /**
     * The recorder used for recording the metrics of this {@link #action}.
     */
    private final ActionMetricsRecorder recorder;

    /**
     * Creates and starts a new {@link ActionScopeImpl} for the given {@link IHookAction}
     *
     * @param action   The {@link IHookAction} of this scope
     * @param recorder The {@link ActionMetricsRecorder}
     */
    public ActionScopeImpl(IHookAction action, ActionMetricsRecorder recorder) {
        this.action = action;
        this.recorder = recorder;

        // set start time of the action/scope in nanoseconds
        startTimeNanos = System.nanoTime();
    }

    @Override
    public void close() {
        // record the action's metrics.
        long executionTimeMicros = (long) ((System.nanoTime() - startTimeNanos) / 1000D);
        recorder.record(action.getName(), executionTimeMicros);
    }
}
