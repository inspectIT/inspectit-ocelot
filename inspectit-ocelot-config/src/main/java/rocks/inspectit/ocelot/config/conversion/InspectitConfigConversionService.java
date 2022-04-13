package rocks.inspectit.ocelot.config.conversion;

import org.springframework.boot.convert.ApplicationConversionService;

/**
 * Extended conversion service to allow shortcuts within the inspectIT configuration.
 */
public class InspectitConfigConversionService extends ApplicationConversionService {

    private static final InspectitConfigConversionService instance = new InspectitConfigConversionService();

    public static InspectitConfigConversionService getInstance() {
        return instance;
    }

    private InspectitConfigConversionService() {
        super();
        addConverter(new StringToMetricRecordingSettingsConverter());
        addConverter(new NumberToMetricRecordingSettingsConverter());
        addConverter(new BooleanToExporterEnabledStateConverter());
        addConverter(new StringToExporterEnabledStateConverter());
    }
}
