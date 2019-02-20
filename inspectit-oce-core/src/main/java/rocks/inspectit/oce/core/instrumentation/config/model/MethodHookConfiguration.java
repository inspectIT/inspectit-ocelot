package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Builder;
import lombok.Value;

/**
 * The configuration used to build a {@link rocks.inspectit.oce.core.instrumentation.hook.MethodHook}
 * Note that the {@link #equals(Object)} function on this class is used to decide whether a recreation of the hook is required.
 */
@Builder
@Value
public class MethodHookConfiguration {
}
