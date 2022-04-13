package rocks.inspectit.ocelot.config.model.exporters;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * The transport protocols used by metrics and tracing exporters.
 */
public enum TransportProtocol {
    UNKNOWN("unknown"), UNSET(""), GRPC("grpc"), HTTP_THRIFT("http/thrift"), HTTP_PROTOBUF("http/protobuf"), HTTP_JSON("http/json"), COUNT("cnt");

    @Getter
    private final String name;

    TransportProtocol(String name) {
        this.name = name;
    }

    static Map<String, TransportProtocol> names = new HashMap<>();

    static {
        for (TransportProtocol tp : TransportProtocol.values()) {
            names.put(tp.getName(), tp);
        }
    }

    /**
     * Parses the given name into a {@link TransportProtocol}
     *
     * @param name The {@link TransportProtocol#name}
     *
     * @return The {@link TransportProtocol} with the given name
     */
    public static TransportProtocol parse(String name) {
        return names.containsKey(name) ? names.get(name) : TransportProtocol.valueOf(name);
    }
}
