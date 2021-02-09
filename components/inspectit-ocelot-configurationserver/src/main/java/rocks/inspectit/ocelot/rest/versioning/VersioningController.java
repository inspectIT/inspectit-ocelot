package rocks.inspectit.ocelot.rest.versioning;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for accessing version information.
 */
@Slf4j
@RestController
public class VersioningController extends AbstractBaseController {

    @Autowired
    private FileManager fileManager;

    @Secured(UserRoleConfiguration.READ_ACCESS_ROLE)
    @ApiOperation(value = "List versions", notes = "Lists all versions which are existing in the configuration server. By default, only versions in the workspace branch will be considered.")
    @GetMapping(value = "versions")
    public List<WorkspaceVersion> listVersions(@RequestParam(name = "limit", required = false) Integer limit) throws IOException, GitAPIException {
        if (limit == null || limit < 0) {
            return fileManager.listWorkspaceVersions();
        }

        return fileManager.listWorkspaceVersions().stream().limit(limit).collect(Collectors.toList());
    }

}
