package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Handler {

    String id;

    String kind;

    String match;

    Object options;

    /**
     * @return a JSON-Object which can be used for adding or updating this handler in POST/PATCH request to kapacitor
     */
    public ObjectNode toKapacitorRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        if (id != null) {
            result.put("id", id);
        }
        if (kind != null) {
            result.put("kind", kind);
        }
        if (options != null) {
            result.set("options", mapper.valueToTree(options));
        }
        if (match != null) {
            result.set("match", mapper.valueToTree(match));
        }
        return result;
    }

    /**
     * Reads a Handler from a kapacitor JSON response.
     *
     * @param node the response from kapacitor
     *
     * @return the parsed handler.
     */
    public static Handler fromKapacitorResponse(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        return Handler.builder()
                .id(node.path("id").asText(null))
                .kind(node.path("kind").asText(null))
                .match(node.path("match").asText(null))
                .options(mapper.convertValue(node.path("options"), Object.class))
                .build();
    }
}
