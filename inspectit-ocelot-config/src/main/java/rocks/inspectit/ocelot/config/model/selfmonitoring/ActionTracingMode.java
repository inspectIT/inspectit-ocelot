package rocks.inspectit.ocelot.config.model.selfmonitoring;

/**
 * Available action tracing modes.
 */
public enum ActionTracingMode {

    /**
     * The action tracing is turned off.
     */
    OFF,

    /**
     * Only actions of rules are traced where the action tracing has been explicitly enabled.
     */
    ONLY_ENABLED,

    /**
     * All actions of rules which are not marked as default rules will be traced.
     */
    ALL_WITHOUT_DEFAULT,

    /**
     * All actions are traced.
     */
    ALL_WITH_DEFAULT;
}
