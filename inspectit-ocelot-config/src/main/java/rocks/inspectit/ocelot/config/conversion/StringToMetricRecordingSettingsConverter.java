package rocks.inspectit.ocelot.config.conversion;


import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

public class StringToMetricRecordingSettingsConverter implements Converter<String, MetricRecordingSettings> {

    @Override
    public MetricRecordingSettings convert(String source) {
        MetricRecordingSettings result = new MetricRecordingSettings();
        result.setValue(source);
        return result;
    }
}
