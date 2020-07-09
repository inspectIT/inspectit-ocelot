package rocks.inspectit.ocelot.config.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

@Slf4j
@Deprecated
public class NumberToMetricRecordingSettingsConverter implements Converter<Number, MetricRecordingSettings> {

    @Override
    public MetricRecordingSettings convert(Number source) {
        // log deprecation warn
        log.warn("You are using the deprecated map-based configuration style for recording metrics. This style will be invalid in future ocelot releases.");

        MetricRecordingSettings result = new MetricRecordingSettings();
        result.setValue(source.toString());
        return result;
    }
}
