package rocks.inspectit.ocelot.config.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

/**
 * Before version 1.15, the enabled property of exporters was a Boolean, now it is a {@link ExporterEnabledState}.
 * To support not-yet-updated configurations, this converter converts the old Boolean values to their equivalent new values,
 * i.e. 'true' to 'IF_CONFIGURED' and 'false' to 'DISABLED'.
 * The class is deprecated, since this conversion functionality will be removed and configurations using the old style
 * made invalid in a future release of InspectIT Ocelot.
 */
@Slf4j
@Deprecated
public class BooleanToExporterEnabledStateConverter implements Converter<Boolean, ExporterEnabledState> {

    @Override
    public ExporterEnabledState convert(Boolean source) {
        // log deprecation warn
        log.warn("You are using the deprecated Boolean-based style to define whether an exporter is enabled. This style will be invalid in future releases of InspectIT Ocelot.");

        if (source) {
            return ExporterEnabledState.IF_CONFIGURED;
        } else {
            return ExporterEnabledState.DISABLED;
        }
    }
}