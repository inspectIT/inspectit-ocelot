package rocks.inspectit.ocelot.config.model.exporters;

/**
 * Enum to change behaviour of exporters.
 * If set to {@link #ENABLED}, the exporter will try to start. If any needed settings are not set, the starting will be aborted and a warning logged.
 * If set to {@link #IF_CONFIGURED}, the exporter will try to start if all needed settings are set, otherwise it will silently stay disabled.
 * If set to {@link #DISABLED}, the exporter will not start.
 */
public enum ExporterEnabledState {
    /**
     * The exporter will try to start. If not all necessary settings are set, starting the exporter will fail with warnings.
     */
    ENABLED,
    /**
     * The exporter will not be started.
     */
    DISABLED,
    /**
     * The exporter will try to start as soon as all necessary settings are set, otherwise it will silently stay disabled
     */
    IF_CONFIGURED;

    /**
     * Returns whether the {@link ExporterEnabledState} results in that the related exporter is disabled.
     * @return Whether the {@link ExporterEnabledState} results in that the related exporter is disabled.
     */
    public boolean isDisabled(){
        // we only have one DISABLED state
        return this.equals(DISABLED);
    }
}
