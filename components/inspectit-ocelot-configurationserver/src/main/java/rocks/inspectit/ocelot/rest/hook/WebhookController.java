package rocks.inspectit.ocelot.rest.hook;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.io.IOException;

@RestController
public class WebhookController extends AbstractBaseController {

    @Autowired
    private FileManager fileManager;

    @ApiOperation(value = "Triggers a synchronization of the workspace branch with a configured remote configuration source.")
    @ApiImplicitParam(name = "token", type = "string", value = "Token for authenticating the request.")
    @GetMapping(value = "hook/synchronize-workspace")
    public ResponseEntity<?> synchronizeWorkspace() throws GitAPIException, IOException {
        fileManager.synchronizeWorkspace();

        return ResponseEntity.ok().build();
    }
}
