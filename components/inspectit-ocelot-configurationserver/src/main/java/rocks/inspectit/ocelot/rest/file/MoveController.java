package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileMoveDescription;

import java.io.IOException;

/**
 * Controller for managing the configurations.
 */
@RestController
public class MoveController extends FileBaseController {

    @ApiOperation(value = "Move or rename a file or directory")
    @PutMapping(value = "move")
    public void moveAssetOrDirectory(@RequestBody FileMoveDescription moveDescription) throws IOException {
        fileManager.move(moveDescription.getSource(), moveDescription.getTarget());
    }

}
