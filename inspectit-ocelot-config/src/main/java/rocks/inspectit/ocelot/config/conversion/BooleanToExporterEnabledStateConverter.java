package rocks.inspectit.ocelot.config.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

@Slf4j
@Deprecated
public class BooleanToExporterEnabledStateConverter implements Converter<Boolean, ExporterEnabledState> {

    @Override
    public ExporterEnabledState convert(Boolean source) {
        // log deprecation warn
        log.warn("You are using the deprecated Boolean-based style to define whether an exporter is enabled. This style will be invalid in future ocelot releases.");

        if (source) {
            return ExporterEnabledState.IF_CONFIGURED;
        } else {
            return ExporterEnabledState.DISABLED;
        }
    }
}