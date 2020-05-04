package rocks.inspectit.ocelot.rest.configschema;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.configschema.ConfigurationPropertyDescription;
import rocks.inspectit.ocelot.configschema.ConfigurationSchemaProvider;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

/**
 * The rest controller providing the interface used by the frontend server for autocomplete function.
 */
@RestController()
public class ConfigSchemaController extends AbstractBaseController {

    @Autowired
    ConfigurationSchemaProvider provider;

    @ApiOperation(value = "Returns a schema describing all plain properties")
    @ApiResponse(code = 200, message = "A JSON object describing the configuration schema for all plain properties")
    @GetMapping("/schema/plain")
    public ConfigurationPropertyDescription getPlainSchema() {
        return provider.getSchema();
    }
}


