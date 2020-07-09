package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

/**
 * Structure representing a Kapacitor Variable or Variable Definition.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateVariable {

    /**
     * This is a special field used by the inspectIT UI.
     * It's description is used as description for the template task.
     * It's value within tasks can be used to describe the task.
     */
    public static final String TEMPLATE_DESCRIPTION_VARIABLE = "inspectit_template_description";

    /**
     * This is a special field used by the inspectIT UI.
     * It references a topic, to which the alerts will be sent.
     */
    public static final String TOPIC_VARIABLE = "topic";

    String name;

    String description;

    String type;

    /**
     * The value of the variable, can be a boolean, number or string.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Object value;

    public static TemplateVariable fromKapacitorResponse(String name, JsonNode variable) {
        return TemplateVariable.builder()
                .name(name)
                .type(variable.path("type").asText(null))
                .description(variable.path("description").asText(""))
                .value(toValue(variable.path("value")))
                .build();
    }

    private static Object toValue(JsonNode value) {
        if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return value.booleanValue();
        } else if (value.isLong()) {
            return value.longValue();
        } else if (value.isNumber()) {
            return value.doubleValue();
        } else if (value.isTextual()) {
            return value.textValue();
        }
        throw new IllegalArgumentException("Unexpected type: " + value);
    }
}
