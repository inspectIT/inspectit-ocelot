package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Kapacitor Topic.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Topic {

    String id;

    /**
     * The current alerting level.
     */
    String level;

    /**
     * Reads a Topic from a kapacitor JSON response.
     *
     * @param node the response from kapacitor
     *
     * @return the parsed topic.
     */
    public static Topic fromKapacitorResponse(JsonNode node) {
        return Topic.builder()
                .id(node.path("id").asText())
                .level(node.path("level").asText())
                .build();
    }
}
