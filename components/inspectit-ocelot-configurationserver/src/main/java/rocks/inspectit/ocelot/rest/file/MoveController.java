package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
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
    public void moveFileOrDirectory(@RequestBody FileMoveDescription moveDescription) throws IOException {
        //remove leading slashes
        String source = moveDescription.getSource();
        String target = moveDescription.getTarget();
        if (StringUtils.startsWith(source, "/")) {
            source = source.substring(1);
        }
        if (StringUtils.startsWith(target, "/")) {
            target = target.substring(1);
        }
        fileManager.move(source, target);
    }

}
