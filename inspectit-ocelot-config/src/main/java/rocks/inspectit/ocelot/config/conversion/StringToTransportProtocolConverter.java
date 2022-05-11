package rocks.inspectit.ocelot.config.conversion;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

/**
 * A {@link Converter} to convert from the String-representation to a {@link TransportProtocol}.
 * This custom {@link Converter} is needed as we are using slash ('/') in the values used in the configuration, e.g., 'http/thrift'.
 */
public class StringToTransportProtocolConverter implements Converter<String, TransportProtocol> {

    @Override
    public TransportProtocol convert(String source) {
        return StringUtils.hasText(source) ? TransportProtocol.parse(source) : null;
    }
}
