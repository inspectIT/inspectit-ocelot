package rocks.inspectit.ocelot.config.conversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

@Slf4j
@Deprecated
public class StringToMetricRecordingSettingsConverter implements Converter<String, MetricRecordingSettings> {

    @Override
    public MetricRecordingSettings convert(String source) {
        // log deprecation warn
        log.warn("You are using the deprecated map-based configuration style for recording metrics. This style will be invalid in future ocelot releases.");

        MetricRecordingSettings result = new MetricRecordingSettings();
        result.setValue(source);
        return result;
    }
}
