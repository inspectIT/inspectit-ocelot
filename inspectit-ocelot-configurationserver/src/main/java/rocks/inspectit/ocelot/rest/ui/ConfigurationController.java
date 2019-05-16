package rocks.inspectit.ocelot.rest.ui;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;

/**
 * Controller for managing the configurations.
 */
@RestController
public class ConfigurationController extends AbstractBaseController {

    @GetMapping("/configurations")
    @ApiOperation(value = "Fetch all configurations", notes = "This endpoint is used by the UI to fetch all existing configuration files.")
    public Collection<String> fetchConfigurations() {
        // ###########################################
        // THIS IS JUST A DUMMY IMPLEMENTATION

        // ###########################################
        return Arrays.asList("/rootFile", "/directory/file");
    }
}
