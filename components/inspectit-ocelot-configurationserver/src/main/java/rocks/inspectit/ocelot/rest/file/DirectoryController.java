package rocks.inspectit.ocelot.rest.file;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.file.FileInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;

/**
 * Controller for managing the configurations.
 */
@RestController
public class DirectoryController extends FileBaseController {


    @GetMapping(value = "directories/**")
    public Collection<FileInfo> listContents(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        return files.getFilesInDirectory(path);
    }


    @PutMapping(value = "directories/**")
    public void createNewDirectory(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        files.createNewDirectory(path);
    }

    @DeleteMapping(value = "directories/**")
    public void deleteDirectory(HttpServletRequest request) throws IOException {
        String path = getRequestSubPath(request);
        files.deleteDirectory(path);
    }

}
