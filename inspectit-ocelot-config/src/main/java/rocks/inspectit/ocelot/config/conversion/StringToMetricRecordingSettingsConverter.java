package rocks.inspectit.ocelot.config.conversion;


import org.springframework.core.convert.converter.Converter;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

// TODO Do we keep this & number converter, or we let users fails with the old style string: string metric config
public class StringToMetricRecordingSettingsConverter implements Converter<String, MetricRecordingSettings> {

    @Override
    public MetricRecordingSettings convert(String source) {
        MetricRecordingSettings result = new MetricRecordingSettings();
        result.setValue(source);
        return result;
    }
}
