package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Template;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class KapacitorTemplateController extends KapacitorBaseController {

    @Autowired
    public KapacitorTemplateController(InspectitServerSettings settings) {
        super(settings);
    }

    @ApiOperation(value = "Provides a list with basic information about each kapacitor template")
    @GetMapping("/alert/kapacitor/templates")
    public List<Template> getAllTemplates() {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/templates", ObjectNode.class)
                .getBody();

        return StreamSupport.stream(response.path("templates").spliterator(), false)
                .map(Template::fromKapacitorResponse)
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Provides detailed information about a given kapacitor template")
    @GetMapping("/alert/kapacitor/templates/{templateId}")
    public Template getTemplate(@PathVariable @ApiParam("The id of the template to query") String templateId) {
        ObjectNode response = kapacitor()
                .getForEntity("/kapacitor/v1/templates/{templateId}", ObjectNode.class, templateId)
                .getBody();

        return Template.fromKapacitorResponse(response);
    }
}
