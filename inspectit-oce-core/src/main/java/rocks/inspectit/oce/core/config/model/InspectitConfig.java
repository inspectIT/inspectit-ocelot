package rocks.inspectit.oce.core.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.config.ConfigSettings;
import rocks.inspectit.oce.core.config.model.exporters.ExportersSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.logging.LoggingSettings;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.oce.core.config.model.selfmonitoring.SelfMonitoringSettings;
import rocks.inspectit.oce.core.config.model.tags.TagsSettings;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Root element of the configuration model for inspectIT.
 * The loading of the configuration is managed by the {@link InspectitEnvironment}.
 * <p>
 * The default values and the structure of the configuration can be found in the /config/default.yml file.
 * Note that in configuration files fields have tobe referred to in kebab-case instead of camelCase!
 * <p>
 * Instances of this class should be treated as values, therefore the setters should never be called!
 * The setters have to be there to work with  the {@link org.springframework.boot.context.properties.bind.Binder}.
 *
 * @author Jonas Kunz
 */
@Data
@NoArgsConstructor
public class InspectitConfig {

    /**
     * The (symbolic) name of the service being instrumented
     */
    @NotBlank
    private String serviceName;

    /**
     * Common tags settings.
     */
    @Valid
    private TagsSettings tags;

    /**
     * Defines all configuration sources.
     */
    @Valid
    private ConfigSettings config = new ConfigSettings();

    /**
     * Settings for all OpenCensus exporters.
     */
    @Valid
    private ExportersSettings exporters = new ExportersSettings();

    /**
     * General metrics settings.
     */
    @Valid
    private MetricsSettings metrics = new MetricsSettings();

    /**
     * General logging settings.
     */
    @Valid
    private LoggingSettings logging;

    /**
     * Settings for the self monitoring.
     */
    @Valid
    private SelfMonitoringSettings selfMonitoring;

    @Valid
    private InstrumentationSettings instrumentation;

    /**
     * Defines how many threads inspectIT may start for its internal tasks.
     */
    @Min(1)
    private int threadPoolSize;

    /**
     * If true, the OpenCensus API and Implementation will be loaded by the bootstrap classloader.
     * Otherwise they will be loaded by the private inspectIT classloader.
     */
    private boolean publishOpencensusToBootstrap;

}
