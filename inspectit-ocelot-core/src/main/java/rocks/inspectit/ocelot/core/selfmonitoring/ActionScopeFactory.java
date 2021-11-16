package rocks.inspectit.ocelot.core.selfmonitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * A singleton factory that creates {@link IActionScope} for a specific action.
 */
@Slf4j
@Component
public class ActionScopeFactory {

    @Autowired
    ActionsMetricsRecorder recorder;

    /**
     * Creates and returns a new {@link IActionScope} for the given {@link IHookAction}. If the {@link ActionsMetricsRecorder} is disabled, the {@link NoopActionScope} will be returned.
     *
     * @param action The action
     *
     * @return A new {@link IActionScope} for the given {@link IHookAction}. A {@link NoopActionScope} is returned if the {@link ActionsMetricsRecorder} is disabled.
     */
    public IActionScope getScope(IHookAction action) {
        return recorder.isEnabled() ? new ActionScopeImpl(action, recorder) : NoopActionScope.INSTANCE;
    }

}
