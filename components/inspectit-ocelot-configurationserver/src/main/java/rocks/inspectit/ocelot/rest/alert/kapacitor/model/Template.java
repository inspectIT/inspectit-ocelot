package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Object representing a Kapacitor template.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Template {

    String id;

    String created;

    String modified;

    String description;

    Boolean hasTopicVariable;

    List<TemplateVariable> vars;

    public static Template fromKapacitorResponse(JsonNode template) {
        Template.TemplateBuilder builder = Template.builder()
                .id(template.path("id").asText())
                .created(template.path("created").asText(null))
                .modified(template.path("modified").asText(null));
        if (template.has("vars")) {
            decodeVariables(template.path("vars"), builder);
        }
        return builder.build();
    }

    private static void decodeVariables(JsonNode variables, Template.TemplateBuilder builder) {
        builder.description("").hasTopicVariable(false); //setup a defaults
        List<TemplateVariable> resultVars = new ArrayList<>();
        Iterator<String> nameIterator = variables.fieldNames();
        while (nameIterator.hasNext()) {
            String name = nameIterator.next();
            JsonNode var = variables.path(name);
            if (TemplateVariable.TEMPLATE_DESCRIPTION_VARIABLE.equals(name)) {
                builder.description(var.path("description").asText());
            } else if (TemplateVariable.TOPIC_VARIABLE.equals(name)) {
                builder.hasTopicVariable(true);
            } else {
                resultVars.add(TemplateVariable.fromKapacitorResponse(name, var));
            }
        }
        builder.vars(resultVars);
    }

}
