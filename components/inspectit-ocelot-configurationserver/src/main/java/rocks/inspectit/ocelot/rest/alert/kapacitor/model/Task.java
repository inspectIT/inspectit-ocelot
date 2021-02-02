package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a single Kapacitor task.
 * Only supports task properties of template-based tasks.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class Task {

    private static final ObjectMapper mapper = new ObjectMapper();

    String id;

    /**
     * Does not actually exist in kapacitor as first-level entity.
     * Instead, this value is assumed to be stored in the Variable {@link TemplateVariable#TOPIC_VARIABLE};
     */
    String topic;

    /**
     * Does not actually exist in kapacitor as first-level entity.
     * Instead, this value is assumed to be stored in the Variable {@link TemplateVariable#TEMPLATE_REFERENCE_VARIABLE};
     */
    String template;

    String status;

    /**
     * Does not actually exist in Kapacitor as first-level entity.
     * Instead, this value is assumed to be stored in the Variable {@link TemplateVariable#TEMPLATE_DESCRIPTION_VARIABLE};
     */
    String description;

    String error;

    String created;

    String modified;

    @JsonProperty("last-enabled")
    String lastEnabled;

    Boolean executing;

    List<TemplateVariable> vars;

    /**
     * @return a JSON-Object which can be used for adding or updating this task in POST/PATCH request to kapacitor
     */
    public ObjectNode toKapacitorRequest() {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode varsNode = mapper.createObjectNode();
        if (id != null) {
            result.put("id", id);
        }
        if (status != null) {
            result.put("status", status);
        }
        if (template != null) {
            result.put("template-id", template);
            TemplateVariable.builder()
                    .name(TemplateVariable.TEMPLATE_REFERENCE_VARIABLE)
                    .type("string")
                    .value(template)
                    .build()
                    .insertIntoKapacitorVars(mapper, varsNode);
        }
        if (topic != null) {
            TemplateVariable.builder()
                    .name(TemplateVariable.TOPIC_VARIABLE)
                    .type("string")
                    .value(topic)
                    .build()
                    .insertIntoKapacitorVars(mapper, varsNode);
        }
        if (description != null) {
            TemplateVariable.builder()
                    .name(TemplateVariable.TEMPLATE_DESCRIPTION_VARIABLE)
                    .type("string")
                    .value(description)
                    .build()
                    .insertIntoKapacitorVars(mapper, varsNode);
        }
        if (vars != null) {
            vars.forEach(var -> var.insertIntoKapacitorVars(mapper, varsNode));
        }
        if (varsNode.size() > 0) {
            result.set("vars", varsNode);
        }
        return result;

    }

    /**
     * @return a JSON-Object which can be used for updating the template in PATCH request to kapacitor
     */
    public ObjectNode toKapacitorRequestTemplateUpdate() {
        ObjectNode result = mapper.createObjectNode();
        result.put("id", template);

        return result;

    }

    public static Task fromKapacitorResponse(JsonNode task) {
        Task.TaskBuilder builder = Task.builder()
                .id(task.path("id").asText())
                .status(task.path("status").asText(null))
                .error(task.path("error").asText(null))
                .created(task.path("created").asText(null))
                .modified(task.path("modified").asText(null))
                .lastEnabled(task.path("last-enabled").asText(null))
                .executing(getOptionalBoolean(task, "executing"));
        if (task.has("vars")) {
            decodeVariables(task.path("vars"), builder);
        }
        if (task.has("template-id")) {
            //override the template setting from the dummy variable with teh actual value if available
            builder.template(task.get("template-id").asText());
        }
        return builder.build();
    }

    private static Boolean getOptionalBoolean(JsonNode node, String path) {
        JsonNode subNode = node.path(path);
        return subNode.isNull() ? null : subNode.booleanValue();
    }

    private static void decodeVariables(JsonNode variables, Task.TaskBuilder builder) {
        List<TemplateVariable> resultVars = new ArrayList<>();
        Iterator<String> nameIterator = variables.fieldNames();
        while (nameIterator.hasNext()) {
            String name = nameIterator.next();
            JsonNode var = variables.path(name);
            if (TemplateVariable.TEMPLATE_DESCRIPTION_VARIABLE.equals(name)) {
                builder.description(var.path("value").asText());
            } else if (TemplateVariable.TOPIC_VARIABLE.equals(name)) {
                builder.topic(var.path("value").asText());
            } else if (TemplateVariable.TEMPLATE_REFERENCE_VARIABLE.equals(name)) {
                builder.template(var.path("value").asText());
            } else {
                resultVars.add(TemplateVariable.fromKapacitorResponse(name, var));
            }
        }
        builder.vars(resultVars);
    }
}
