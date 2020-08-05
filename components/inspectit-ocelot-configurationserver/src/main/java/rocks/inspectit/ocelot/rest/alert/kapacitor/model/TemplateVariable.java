package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.convert.DurationStyle;
import rocks.inspectit.ocelot.rest.util.DurationUtil;

import java.time.Duration;

/**
 * Structure representing a Kapacitor Variable or Variable Definition.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class TemplateVariable {

    /**
     * This is a special field used by the inspectIT UI.
     * It's description is used as description for the template task.
     * It's value within tasks can be used to describe the task.
     */
    public static final String TEMPLATE_DESCRIPTION_VARIABLE = "inspectit_template_description";

    /**
     * This is a special field used by the inspectIT UI.
     * It is used to store the reference to the template from which this task is constructed.
     */
    public static final String TEMPLATE_REFERENCE_VARIABLE = "inspectit_template_reference";

    /**
     * This is a special field used by the inspectIT UI.
     * It references a topic, to which the alerts will be sent.
     */
    public static final String TOPIC_VARIABLE = "topic";

    private static final String DURATION_TYPE = "duration";

    String name;

    String description;

    String type;

    /**
     * The value of the variable, can be a boolean, number or string.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Object value;

    /**
     * Inserts thius variable into the provided "vars" Object node.
     * This method can be used to add this variable to the "vars" of a given task.
     *
     * @param mapper   the ObjectMapper for creating new nodes
     * @param varsNode the "vars" map in which the variable definition will be placed.
     */
    public void insertIntoKapacitorVars(ObjectMapper mapper, ObjectNode varsNode) {
        ObjectNode subNode = mapper.createObjectNode();
        subNode.put("type", type);
        subNode.set("value", convertValueForKapacitor(mapper));
        varsNode.set(name, subNode);
    }

    private JsonNode convertValueForKapacitor(ObjectMapper mapper) {
        if (DURATION_TYPE.equals(type) && value instanceof String) {
            Duration duration = DurationStyle.SIMPLE.parse((String) value);
            return mapper.valueToTree(duration.toNanos());
        } else {
            return mapper.valueToTree(value);
        }
    }

    public static TemplateVariable fromKapacitorResponse(String name, JsonNode variable) {
        String type = variable.path("type").asText(null);
        return TemplateVariable.builder()
                .name(name)
                .type(type)
                .description(variable.path("description").asText(""))
                .value(toValue(variable.path("value"), type))
                .build();
    }

    private static Object toValue(JsonNode value, String type) {
        if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return value.booleanValue();
        } else if (value.isLong()) {
            if (DURATION_TYPE.equals(type)) {
                return DurationUtil.prettyPrintDuration(Duration.ofNanos(value.longValue()));
            } else {
                return value.longValue();
            }
        } else if (value.isNumber()) {
            return value.doubleValue();
        } else if (value.isTextual()) {
            return value.textValue();
        }
        throw new IllegalArgumentException("Unexpected type: " + value);
    }

}
