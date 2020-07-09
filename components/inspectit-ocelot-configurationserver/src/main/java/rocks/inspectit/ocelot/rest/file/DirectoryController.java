package rocks.inspectit.ocelot.rest.file;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileInfo;
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

    @ApiOperation(value = "List directory contents", notes = "Can be used to get a list of the contents of a given directory.")
    @ApiImplicitParam(name = "Path", value = "The part of the url after /directories/ define the path to the directory whose contents shall be read.")
    @GetMapping(value = "directories/**")
    public Collection<FileInfo> listContents(HttpServletRequest request) {
        String path = RequestUtil.getRequestSubPath(request);
        return fileManager.getWorkingDirectory().listConfigurationFiles(path);
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
