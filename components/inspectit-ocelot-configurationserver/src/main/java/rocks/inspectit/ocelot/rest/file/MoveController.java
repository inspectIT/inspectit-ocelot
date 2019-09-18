package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    public void moveFileOrDirectory(@RequestBody FileMoveDescription moveDescription) throws IOException, GitAPIException {
        String source = removeLeadingSlash(moveDescription.getSource());
        String target = removeLeadingSlash(moveDescription.getTarget());
        fileManager.move(source, target);
    }

    private String removeLeadingSlash(String path) {
        if (StringUtils.startsWith(path, "/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }

}
