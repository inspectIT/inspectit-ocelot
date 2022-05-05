package rocks.inspectit.ocelot.config.model.exporters;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * The transport protocols used by metrics and tracing exporters.
 * As we adhere to the naming convention of {@link https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md OpenTelemetry SDK Autoconfigure}, which uses '/'' in the protocl values,
 * e.g., 'http/thrift' or 'http/protobuf'.
 * To use the same value convention in our configuration, we need the custom {@link rocks.inspectit.ocelot.config.conversion.StringToTransportProtocolConverter} and the custom {@link #parse(String)} method that uses the underlying {@link #configRepresentation}.
 */
public enum TransportProtocol {
    UNSET(""), GRPC("grpc"), HTTP_THRIFT("http/thrift"), HTTP_PROTOBUF("http/protobuf");

    /**
     * The representation of the {@link TransportProtocol} used in configuration files.
     * We use this additional property to support the value convention of OpenTelemetry, see class comment.
     */
    @Getter
    private final String configRepresentation;

    TransportProtocol(String configVal) {
        configRepresentation = configVal;
    }

    static Map<String, TransportProtocol> values = new HashMap<>();

    static {
        for (TransportProtocol tp : TransportProtocol.values()) {
            values.put(tp.getConfigRepresentation(), tp);
        }
    }

    /**
     * Parses the given config value representation into a {@link TransportProtocol}
     *
     * @param configVal The {@link TransportProtocol#configRepresentation}
     *
     * @return The {@link TransportProtocol} with the given config value.
     */
    public static TransportProtocol parse(String configVal) {
        return values.containsKey(configVal) ? values.get(configVal) : TransportProtocol.valueOf(configVal.toUpperCase());
    }

    @Override
    public String toString() {
        // Override the default toString() method to return the config string representation.
        // This is needed for the TransportProtocolSubset and TransportProtocolSubsetValidator, which use the toStringMethod in the validation message.
        return configRepresentation;
    }
}
