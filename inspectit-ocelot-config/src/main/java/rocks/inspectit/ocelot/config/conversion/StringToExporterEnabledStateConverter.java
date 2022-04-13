package rocks.inspectit.ocelot.config.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

/**
 * Before version 1.15, the enabled property of exporters was a Boolean, now it is a {@link ExporterEnabledState}.
 * To support not-yet-updated configurations, this {@link BooleanToExporterEnabledStateConverter} converts Boolean values
 * to their equivalent new values, i.e. 'true' to 'IF_CONFIGURED' and 'false' to 'DISABLED'.
 * However, in Spring Booleans can be defined using Strings, i.e. quoted Boolean values like '"false"', as well, so this
 * converter is additionally needed to cover that case. However, because the correct new values are also Strings at first,
 * that are then converted to ExporterEnabledStates, this class also needs to handle that correct usage.
 * The class is deprecated, since the Boolean conversion functionality will be removed and configurations using the old style
 * made invalid in a future release of InspectIT Ocelot, and the conversion for correct values will without this
 * Converter work out-of-the-box in Spring.
 */
@Slf4j
@Deprecated
public class StringToExporterEnabledStateConverter implements Converter<String, ExporterEnabledState> {

    @Override
    public ExporterEnabledState convert(String source) {
        if (source.equalsIgnoreCase("true") || source.equalsIgnoreCase("false")) {
            // log deprecation warn
            log.warn("You are using the deprecated Boolean-based style with quotes to define whether an exporter is enabled. This style will be invalid in future releases of InspectIT Ocelot.");
            return source.equalsIgnoreCase("true") ? ExporterEnabledState.IF_CONFIGURED : ExporterEnabledState.DISABLED;
        } else {
            return ExporterEnabledState.valueOf(source);
        }
    }
}