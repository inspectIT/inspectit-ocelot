package rocks.inspectit.ocelot.config.model.exporters;

/**
 * Enum to change behaviour of exporters.
 * If set to ENABLED, the exporter will try to start. If any needed settings are not set, the starting will be aborted and an error logged.
 * If set to IF_CONFIGURED, the exporter will try to start if all needed settings are set, otherwise it will silently stay disabled.
 * If set to DISABLED, the exporter will not start.
 */
public enum ExporterEnabledState {
    ENABLED, DISABLED, IF_CONFIGURED
}
