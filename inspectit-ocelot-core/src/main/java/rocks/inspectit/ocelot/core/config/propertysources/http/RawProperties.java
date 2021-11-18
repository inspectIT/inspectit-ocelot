package rocks.inspectit.ocelot.core.config.propertysources.http;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Class representing the raw inspectit configuration content used to parse {@link java.util.Properties}
 */
@Value
@AllArgsConstructor
public class RawProperties {

    /**
     * The raw configuration as a string
     */
    private String rawProperties;

    /**
     * The MIME type of the configuration string
     */
    private String mimeType;

}
