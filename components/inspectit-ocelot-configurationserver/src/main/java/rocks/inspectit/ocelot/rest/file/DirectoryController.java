package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.rest.util.RequestUtil;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;

/**
 * Controller for managing the configurations.
 */
@RestController
public class DirectoryController extends FileBaseController {

    @ApiOperation(value = "List directory contents", notes = "Can be used to get a list of the contents of a given directory. In addition, the branch can be specified which will be used as basis for the listing.")
    @ApiImplicitParam(name = "Path", value = "The part of the url after /directories/ define the path to the directory whose contents shall be read.")
    @GetMapping(value = "directories/**")
    public Collection<FileInfo> listContents(@ApiParam("The id of the version which should be listed. If it is empty, the lastest workspace version is used. Can be 'live' fir listing the latest live version.") @RequestParam(value = "version", required = false) String commitId, HttpServletRequest request) {
        String path = RequestUtil.getRequestSubPath(request);

        if (commitId == null) {
            // Is used to display empty directories.
            return fileManager.getWorkingDirectory().listConfigurationFiles(path);
        } else if (commitId.equals("live")) {
            return fileManager.getLiveRevision().listConfigurationFiles(path);
        } else {
            return fileManager.getCommitWithId(commitId).listConfigurationFiles(path);
        }
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Create a directory", notes = "Creates a new, empty directory including its parent folders. Does nothing if the directory already exists.")
    @ApiImplicitParam(name = "Path", value = "The part of the url after /directories/ define the path of the directory to create.")
    @PutMapping(value = "directories/**")
    public void createNewDirectory(HttpServletRequest request) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);
        fileManager.getWorkingDirectory().createConfigurationDirectory(path);
    }

    @Secured(UserRoleConfiguration.WRITE_ACCESS_ROLE)
    @ApiOperation(value = "Delete a directory", notes = "Deletes a directory including its contents.")
    @ApiImplicitParam(name = "Path", value = "The part of the url after /directories/ define the path of the directory to delete.")
    @DeleteMapping(value = "directories/**")
    public void deleteDirectory(HttpServletRequest request) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);
        fileManager.getWorkingDirectory().deleteConfiguration(path);
    }

}
