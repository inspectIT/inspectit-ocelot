package rocks.inspectit.ocelot.rest.hook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.io.IOException;

@RestController
public class WebhookController extends AbstractBaseController {

    @Autowired
    private FileManager fileManager;

    @Operation(summary = "Triggers a synchronization of the workspace branch with a configured remote configuration source.")
    @Parameter(name = "token", schema = @Schema(type = "string"), description = "Token for authenticating the request.")
    @RequestMapping(path = {"/hook/synchronize-workspace", "/hook/synchronize-workspace/"}, method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> synchronizeWorkspace() throws GitAPIException, IOException {
        fileManager.synchronizeWorkspace();

        return ResponseEntity.ok().build();
    }
}
