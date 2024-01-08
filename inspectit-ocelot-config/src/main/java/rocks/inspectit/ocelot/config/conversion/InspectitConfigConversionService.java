package rocks.inspectit.ocelot.config.conversion;

import org.springframework.boot.convert.ApplicationConversionService;

/**
 * Extended conversion service to allow shortcuts within the inspectIT configuration.
 * <p>
 * There are two instances of the conversion service: the (default) instance and the parserInstance. <br>
 * The parserInstance should only be used by the 'ConfigParser', since it contains an additional converter,
 * which is necessary, since the ConfigParser also tries to convert single YAML files and thus sometimes cannot resolve
 * YAML placeholders, which reference properties in other YAML files. <br>
 * All other classes should use the (default) instance.
 */
public class InspectitConfigConversionService extends ApplicationConversionService {

    private static final InspectitConfigConversionService instance = new InspectitConfigConversionService();

    private static final InspectitConfigConversionService parserInstance = new InspectitConfigConversionService(new PlaceholderToDurationConverter());

    /**
     * @return the inspectIT default instance
     */
    public static InspectitConfigConversionService getInstance() {
        return instance;
    }

    /**
     * @return the instance, which should only be used by the 'ConfigParser'
     */
    public static InspectitConfigConversionService getParserInstance() {
        return parserInstance;
    }

    private InspectitConfigConversionService() {
        super();
        addInspectItDefaultConverters();
    }

    private InspectitConfigConversionService(PlaceholderToDurationConverter placeHolderConverter) {
        super();
        addInspectItDefaultConverters();
        addConverter(placeHolderConverter);
    }

    private void addInspectItDefaultConverters() {
        addConverter(new StringToMetricRecordingSettingsConverter());
        addConverter(new NumberToMetricRecordingSettingsConverter());
        addConverter(new BooleanToExporterEnabledStateConverter());
        addConverter(new StringToExporterEnabledStateConverter());
        addConverter(new StringToTransportProtocolConverter());
    }
}
