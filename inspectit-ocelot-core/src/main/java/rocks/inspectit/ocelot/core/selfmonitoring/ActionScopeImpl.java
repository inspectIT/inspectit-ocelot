package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

import java.util.concurrent.TimeUnit;

/**
 * Functional implementation of the {@link IActionScope}
 */
@Data
@Slf4j
public class ActionScopeImpl implements IActionScope {

    /**
     * The {@link IHookAction} of this scope.
     */
    private IHookAction action;

    /**
     * The start time of the action/scope in nanoseconds.
     */
    private long startTimeNanos;

    /**
     * The recorder used for recording the metrics of this {@link #action}.
     */
    private ActionsMetricsRecorder recorder;

    /**
     * Creates and starts a new {@link ActionScopeImpl} for the given {@link IHookAction}
     *
     * @param action   The {@link IHookAction} of this scope
     * @param recorder The {@link ActionsMetricsRecorder}
     */
    public ActionScopeImpl(IHookAction action, ActionsMetricsRecorder recorder) {
        this(action, recorder, true);
    }

    /**
     * @param action    The {@link IHookAction} of this scope
     * @param recorder  The {@link ActionsMetricsRecorder}
     * @param autoStart Whether to automatically start the scope, i.e., setting the {@link #startTimeNanos}
     */
    public ActionScopeImpl(IHookAction action, ActionsMetricsRecorder recorder, boolean autoStart) {
        this.action = action;
        this.recorder = recorder;
        if (autoStart) {
            start();
        }
    }

    @Override
    public void start() {
        // set start time of the action/scope in nanoseconds
        startTimeNanos = System.nanoTime();
    }

    @Override
    public void start(long startTimeNanos) {
        this.startTimeNanos = startTimeNanos;
    }

    @Override
    public void close() {
        // record the action's metrics.
        val executionTimeMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTimeNanos);
        // log.info("ActionScopeImpl.close. action={}, startTime={}, endTime={}, executionTimeMillis={}", action.getName(), startTimeNanos, System.nanoTime(), executionTimeMillis);
        recorder.record(action, executionTimeMicros);
    }
}
