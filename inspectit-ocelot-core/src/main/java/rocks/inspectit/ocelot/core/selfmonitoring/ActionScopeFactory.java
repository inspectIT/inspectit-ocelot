package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * A singleton factory that creates {@link IActionScope} for a specific {@link IHookAction}.
 */
@Slf4j
@Component
public class ActionScopeFactory {

    @Autowired
    private ActionMetricsRecorder recorder;

    /**
     * Creates and returns a new {@link IActionScope} for the given {@link IHookAction}.
     * If the {@link ActionMetricsRecorder} is disabled, the {@link IActionScope#NOOP_ACTION_SCOPE}  will be returned.
     *
     * @param action The action
     *
     * @return A new {@link IActionScope} for the given {@link IHookAction} or {@link IActionScope#NOOP_ACTION_SCOPE} in case {@link ActionMetricsRecorder} is disabled.
     */
    public IActionScope createScope(IHookAction action) {
        return recorder.isEnabled() ? new ActionScopeImpl(action, recorder) : IActionScope.NOOP_ACTION_SCOPE;
    }

}
