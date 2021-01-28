package rocks.inspectit.oce.eum.server.beacon;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Serializer which is used to serialize beacons into JSON.
 *
 * Example output:
 * {
 *     beacon: {
 *         ...beacon-content
 *     },
 *     event: {
 *         kind: "event",
 *         provider: "ocelot-eum-server"
 *     }
 * }
 */
public class BeaconECSSerializer extends StdSerializer<Beacon> {

    public BeaconECSSerializer() {
        this(null);
    }

    public BeaconECSSerializer(Class<Beacon> clazz) {
        super(clazz);
    }

    @Override
    public void serialize(Beacon value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeObjectFieldStart("event");
        jgen.writeStringField("kind", "event");
        jgen.writeStringField("provider", "ocelot-eum-server");
        jgen.writeEndObject();

        jgen.writeObjectField("beacon", value.toMap());

        jgen.writeEndObject();
    }
}