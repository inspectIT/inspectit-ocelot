package rocks.inspectit.ocelot.config.conversion;


import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

public class NumberToMetricRecordingSettingsConverter implements Converter<Number, MetricRecordingSettings> {

    @Override
    public MetricRecordingSettings convert(Number source) {
        MetricRecordingSettings result = new MetricRecordingSettings();
        result.setValue(source.toString());
        return result;
    }
}
